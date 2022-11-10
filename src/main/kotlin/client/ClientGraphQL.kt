package client

import client.ClientUtil.getDateFormatterGithub
import client.exceptions.GraphQLException
import client.exceptions.RateLimitException
import com.example.generated.GetPullRequests
import com.example.generated.getpullrequests.PullRequestConnection
import com.example.generated.getpullrequests.Repository
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import entity.rest.github.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


typealias GetPrResponse = GraphQLClientResponse<GetPullRequests.Result>

class ClientGraphQL(private val githubToken: String) {
  private val client = GraphQLKtorClient(
    url = URL("https://api.github.com/graphql"),
    serializer = GraphQLClientKotlinxSerializer()
  )

  //    "Zulu time" (UTC)
  private val dateFormat: SimpleDateFormat = getDateFormatterGithub()

  init {
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
  }


  private fun getBatch(
    githubRepositoryInfo: GithubRepositoryInfo,
    afterPR: String? = null
  ): GetPrResponse {
    return runBlocking {
      val variables = GetPullRequests.Variables(
        owner = githubRepositoryInfo.owner,
        repoName = githubRepositoryInfo.name,
        lastPR = 30,
        firstReviews = 30,
        firstReviewThreads = 30,
        firstReviewThreadComments = 10,
        firstCommits = 30,
        beforePR = afterPR
      )
      val query = GetPullRequests(variables)
      val result = client.execute(query) {
        header("Authorization", "bearer $githubToken")
      }
      result
    }
  }

  private fun filterBatch(batch: PRBatch, upTo: Date? = null): PRBatch {
    if (upTo == null) return batch

    val pullRequests = batch.getPullRequests()

    val filteredPRs = pullRequests.filter {
      val prCreationDate = dateFormat.parse(it.getCreatedAt())
      prCreationDate >= upTo
    }

    if (filteredPRs.size != pullRequests.size) {
      return PRBatch(
        batch.rateLimit,
        Repository(
          PullRequestConnection(
            batch.getPRTotalCount(),
            filteredPRs
          )
        )
      )
    }

    return batch
  }


  fun loadPRs(
    githubRepositoryInfo: GithubRepositoryInfo,
    startingPR: String? = null,
    upTo: Date? = null,
    waitRateLimit: Boolean = false,
    proceedBatch: (PRBatch) -> Unit
  ) {
    var init = true
    var totalAmountOfPRs = Int.MAX_VALUE
    var proceededPR = 0
    var afterPR = startingPR
    while (proceededPR < totalAmountOfPRs) {
      try {
        val response = getBatch(githubRepositoryInfo, afterPR = afterPR)
        response.errors?.let {
          throw GraphQLException(it)
        }

        val batch = response.data!!
        val filteredBatch = filterBatch(batch, upTo = upTo)
        proceedBatch(filteredBatch)

        if (filteredBatch.getPullRequests().size != batch.getPullRequests().size) {
          return
        }

        afterPR = batch.firstCreatedPRCursor()
        proceededPR += batch.size()
        if (init) {
          totalAmountOfPRs = batch.getPRTotalCount()
          init = false
        }

        val rateLimit = batch.getRateLimit()
        if (rateLimit.cost > rateLimit.remaining) {
          if (waitRateLimit) {
            ClientUtil.waitRateLimit(rateLimit)
          } else {
            throw RateLimitException("GraphQL. Operation exceeds rate limit. Next rate limit reset at ${rateLimit.resetAt}.")
          }
        }
      } catch (e: ServerResponseException) {
        println(e)
      } catch (e: SerializationException) {
        println(e)
      } catch (e: Throwable) {
        throw e
      }
    }
  }
}
