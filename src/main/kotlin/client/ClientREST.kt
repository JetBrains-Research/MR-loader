package client

import entity.rest.github.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

class ClientREST(private val githubToken: String, githubRepositoryInfo: GithubRepositoryInfo) {
  companion object {
    private const val API = "https://api.github.com"
  }

  private val client = HttpClient()
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
  private val loginToProfile = HashMap<String, UserProfile>()
  private val apiRepo =
    "$API/repos/${githubRepositoryInfo.owner}/${githubRepositoryInfo.name}"

  private var rateLimit: RateLimitREST? = null
  private var numberOfRequests = 0

  private suspend fun requestRaw(path: String) = client
    .request<String>(path) {
      header("Authorization", "token $githubToken")
    }

  private suspend inline fun <reified T> request(path: String, waitRateLimit: Boolean): T {
    if (waitRateLimit) {
      updateRateLimit()
      val remaining = rateLimit!!.resources.core.remaining
      if (remaining == numberOfRequests) {
        ClientUtil.waitRateLimit(rateLimit!!)
      }
      numberOfRequests++
    }
    return json.decodeFromString(requestRaw(path))
  }

  private suspend fun updateRateLimit() {
    if (rateLimit == null) {
      rateLimit = getRateLimit()
    }

    val currDate = Date()
    val resetDate = Date(rateLimit!!.resources.core.reset.toLong() * 1000)
    if (currDate > resetDate) {
      rateLimit = getRateLimit()
      numberOfRequests = 0
    }

  }

  suspend fun getPullRequest(number: Int) = requestRaw("${apiRepo}/pulls/$number")

  suspend fun getReviewComments(number: Int, waitRateLimit: Boolean = false) =
    request<CommentsREST>("${apiRepo}/pulls/$number/comments", waitRateLimit)

  suspend fun getUserProfile(userLogin: String, waitRateLimit: Boolean = false): UserProfile {
    loginToProfile[userLogin]?.let { return it }
    val userProfile = request<UserProfile>("$API/users/$userLogin", waitRateLimit)
    loginToProfile[userLogin] = userProfile
    return userProfile
  }

  suspend fun getRateLimit() = request<RateLimitREST>("$API/rate_limit", false)

  // shows up to 250 commits
  // for more look into https://docs.github.com/en/rest/reference/commits#list-commits
  suspend fun getCommits(number: Int, waitRateLimit: Boolean = false) =
    request<CommitsREST>("${apiRepo}/pulls/$number/commits", waitRateLimit)
}

