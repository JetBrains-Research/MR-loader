package client

import entity.rest.gerrit.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json

class ClientGerritREST {
  companion object {
    const val START_RESPONSE = ")]}'\n"
    private val CHANGES_PARAMETERS = listOf(
      "DETAILED_LABELS",
      "ALL_FILES",
      "ALL_REVISIONS",
      "ALL_COMMITS",
      "COMMIT_FOOTERS",
      "DETAILED_ACCOUNTS"
    ).joinToString("&o=")

    val json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }
  }

  private val client = HttpClient() {
    install(HttpTimeout) {
      socketTimeoutMillis = 600_000
    }
  }

  suspend fun requestRaw(urlString: String) = client.request(urlString).body<String>().removePrefix(START_RESPONSE)

  private suspend inline fun <reified T> request(urlString: String): T = json.decodeFromString(requestRaw(urlString))

  suspend fun getUserAccount(baseUrl: String, accountId: Int) =
    request<UserAccountGerrit>("$baseUrl/accounts/$accountId")

  suspend fun getReviewers(baseUrl: String, changeId: Int) =
    request<ReviewersREST>("$baseUrl/changes/$changeId/reviewers")

  suspend fun getChanges(baseUrl: String, offset: Int = 0) =
    request<ChangesREST>(getChangesRaw(baseUrl, null, offset))

  suspend fun getChanges(baseUrl: String, before: String, offset: Int = 0) =
    request<ChangesREST>(getChangesRaw(baseUrl, before, offset))

  suspend fun getChange(baseUrl: String, changeId: Int = 0, nChanges: Int = 300) =
    request<ChangeGerrit>("$baseUrl/changes/$changeId/detail?o=$CHANGES_PARAMETERS&n=$nChanges")

  suspend fun getChangesRaw(baseUrl: String, before: String? = null, offset: Int = 0) =
    requestRaw(changesURL(baseUrl, before, offset))

  private fun changesURL(baseUrl: String, before: String?, offset: Int, nChanges: Int = 300) =
    if (before != null)
      "$baseUrl/changes/?q=before:$before&S=$offset&o=$CHANGES_PARAMETERS&n=$nChanges"
    else
      "$baseUrl/changes/?S=$offset&o=$CHANGES_PARAMETERS&n=$nChanges"

  suspend fun getChangesRaw(baseUrl: String, before: String?, after: String?, nChanges: Int = 300) =
    requestRaw("$baseUrl/changes/?${timeGap(before, after)}&o=$CHANGES_PARAMETERS&n=$nChanges")

  suspend fun getChangesRawLight(baseUrl: String, before: String? = null, after: String? = null, nChanges: Int = 300) =
    requestRaw("$baseUrl/changes/?${timeGap(before, after)}&n=$nChanges")

  suspend fun getChangesRawLight(
    baseUrl: String,
    project: String,
    status: String,
    offset: Int,
    before: String? = null
  ): String {
    val beforeQuery = before?.let { "+before:$it" } ?: ""
    val query = "?q=project:$project+status:$status$beforeQuery"
    return requestRaw("$baseUrl/changes/$query&S=$offset")
  }

  suspend fun getChangesRawLightNew(baseUrl: String, offset: Int) =
    requestRaw("$baseUrl/changes/?S=$offset")

  suspend fun getChangesRawLightWithProject(
    baseUrl: String,
    project: String,
    before: String? = null,
    after: String? = null,
    nChanges: Int = 300
  ) =
    requestRaw("$baseUrl/changes/?${timeGapProject(project, before, after)}&n=$nChanges")

  suspend fun getProjects(baseUrl: String) =
    request<Map<String, ProjectGerrit>>("$baseUrl/projects/").keys

  private fun timeGapProject(project: String, before: String? = null, after: String? = null): String {
    var result = "q=project:$project+"
    before?.let {
      result += "+before:$before"
    }
    after?.let {
      result += "+after:$after"
    }
    return result
  }

  suspend fun getChangeRaw(baseUrl: String, number: Int) =
    requestRaw("$baseUrl/changes/$number/?&o=$CHANGES_PARAMETERS")

  private fun timeGap(before: String?, after: String?): String {
    var result = ""
    before?.let { result += "before:$before" }
    after?.let {
      if (result.isNotEmpty()) result += "+"
      result += "after:$after"
    }
    return if (result.isNotEmpty()) "q=$result" else ""
  }

  suspend fun getComments(baseUrl: String, changeId: Int) =
    request<CommentsREST>(commentsURL(baseUrl, changeId))

  suspend fun getCommentsRaw(baseUrl: String, changeId: Int) =
    requestRaw(commentsURL(baseUrl, changeId))

  private fun commentsURL(baseUrl: String, changeId: Int) = "$baseUrl/changes/$changeId/comments"

}
