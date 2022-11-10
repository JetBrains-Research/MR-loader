package loader.github

import client.ClientGraphQL
import client.ClientREST
import com.example.generated.getpullrequests.PullRequestEdge
import entity.rest.github.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

class LoaderPR(private val githubRepositoryInfo: GithubRepositoryInfo, githubToken: String) {

  private data class LoadParametersREST(val number: Int, val loadComments: Boolean, val loadCommits: Boolean)
  private data class InitValues(
    val batchId: Int,
    val loadParametersRESTS: List<LoadParametersREST>,
    val cursor: String?
  )

  private val clientGraphQL = ClientGraphQL(githubToken)
  private val clientREST = ClientREST(githubToken, githubRepositoryInfo)
  private val saveFolder = File("./results/${githubRepositoryInfo.owner}/${githubRepositoryInfo.name}")
  private val batchFolder = File(saveFolder, "batches")
  private val commitsFolder = File(saveFolder, "commits")
  private val commentsFolder = File(saveFolder, "comments")
  private val usersProfileFolder = File(saveFolder, "userProfiles")

  suspend fun load(upTo: Date? = null, waitRateLimit: Boolean = false) {
    val initValues = restoreState()
    println(
      "Init values initBatchId:${initValues.batchId}; " +
          "loadParametersRest:${initValues.loadParametersRESTS}; " +
          "cursor: ${initValues.cursor}"
    )

    var batchId = initValues.batchId
    val loadParametersRESTS = mutableListOf<LoadParametersREST>()
    loadParametersRESTS.addAll(initValues.loadParametersRESTS)

    clientGraphQL.loadPRs(
      githubRepositoryInfo,
      upTo = upTo,
      startingPR = initValues.cursor,
      waitRateLimit = waitRateLimit
    ) { prBatch ->
      val pullRequests = prBatch.getPullRequests()
      println("Loaded batch $batchId.")
      if (pullRequests.isNotEmpty()) {
        println("Numbers ${pullRequests.first().getNumber()} - ${pullRequests.last().getNumber()}")
        saveToFile(prBatch, batchFile(batchId))
        batchId++

        for (pullRequest in pullRequests) {
          getLoadRestOrNull(pullRequest!!)?.let {
            loadParametersRESTS.add(it)
          }
        }
      }
    }

    for (parameters in loadParametersRESTS) {
      if (parameters.loadComments) {
        println("Loading via REST comments of PR: ${parameters.number}")
        val comments = clientREST.getReviewComments(parameters.number, waitRateLimit)
        saveToFile(comments, commentsFile(parameters.number))

//        TODO: extract emails from commits REST result?
        for (comment in comments) {
          val userLogin = comment.user.login
          val userProfile = clientREST.getUserProfile(userLogin, waitRateLimit)
          val file = userProfileFile(userLogin)
          if (!file.exists()) saveToFile(userProfile, file)
        }
      }

      if (parameters.loadCommits) {
        println("Loading via REST commits of PR: ${parameters.number}")
        val commits = clientREST.getCommits(parameters.number, waitRateLimit)
        saveToFile(commits, commitsFile(parameters.number))
      }
    }

  }

  fun processPRs(processPR: (PRInfo) -> Unit) {
    // TODO: add better check
    if (!batchFolder.exists()) return

    for (file in batchFolder.listFiles()) {
      val prBatch = Json.decodeFromString<PRBatch>(file.readText())
      for (pullRequest in prBatch.getPullRequests()) {
        val number = pullRequest.getNumber()
        val prAuthor = pullRequest.getAuthorProfile()

        val reviewers = HashMap<String, UserProfile>()
        pullRequest.getReviews().forEach {
          val profile = it.getAuthorProfile()
          reviewers[profile.login] = profile
        }
        pullRequest.getReviewThreads().forEach {
          it.getComments().forEach { comment ->
            val profile = comment.getAuthorProfile()
            reviewers[profile.login] = profile
          }
        }

        if (!pullRequest.commentsFullyLoaded()) {
          val comments = Json.decodeFromString<CommentsREST>(commentsFile(number).readText())
          comments.forEach {
            val login = it.user.login
            if (login !in reviewers.keys) {
              val userProfile = Json.decodeFromString<UserProfile>(userProfileFile(it.user.login).readText())
              reviewers[login] = userProfile
            }

          }
        }
        reviewers.remove(prAuthor.login)

        val commits = if (pullRequest.commitsIsFullyLoaded()) {
          pullRequest.getCommits().map { it.getHash() }
        } else {
          val commitsREST = Json.decodeFromString<CommitsREST>(commitsFile(number).readText())
          commitsREST.map { it.sha }
        }

        val prInfo = PRInfo(
          pullRequest.getAuthorProfile(),
          pullRequest.getNumber(),
          pullRequest.getCreatedAt(),
          pullRequest.getClosedAt() ?: "",
          reviewers.values.toSet(),
          commits
        )
        processPR(prInfo)
      }
    }
  }

  private fun batchFile(batchId: Int) = File(
    batchFolder, batchId.toString()
  )

  private fun commitsFile(number: Int) = File(
    commitsFolder, number.toString()
  )

  private fun commentsFile(number: Int) = File(
    commentsFolder, number.toString()
  )

  private fun userProfileFile(login: String) = File(
    usersProfileFolder, login
  )

  private inline fun <reified T> saveToFile(data: T, file: File) {
    file.parentFile.mkdirs()
    file.writeText(Json.encodeToString(data))
  }

  private fun getLoadRestOrNull(pullRequest: PullRequestEdge): LoadParametersREST? {
    val number = pullRequest.getNumber()
    val loadComments = !commentsFile(number).exists() && !pullRequest.commentsFullyLoaded()
    val loadCommits = !commitsFile(number).exists() && !pullRequest.commitsIsFullyLoaded()
    if (loadComments || loadCommits) {
      return LoadParametersREST(pullRequest.getNumber(), loadComments, loadCommits)
    }
    return null
  }

  //  TODO: check empty
  private fun restoreState(): InitValues {
    if (!batchFolder.exists()) {
      return InitValues(0, emptyList(), null)
    }

    val latestBatchId = batchFolder.list().maxOfOrNull { it.toInt() }!!
    val loadParametersRESTS = mutableListOf<LoadParametersREST>()
    for (file in batchFolder.listFiles()) {
      val prBatch = Json.decodeFromString<PRBatch>(file.readText())
      for (pullRequest in prBatch.getPullRequests()) {
        getLoadRestOrNull(pullRequest!!)?.let { parameters ->
          loadParametersRESTS.add(parameters)
        }
      }
    }
    val latestCursor = Json.decodeFromString<PRBatch>(batchFile(latestBatchId).readText()).firstCreatedPRCursor()
    return InitValues(latestBatchId + 1, loadParametersRESTS, latestCursor)
  }
}
