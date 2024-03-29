package loader.gerrit

import client.ClientUtil
import entity.rest.gerrit.Change
import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.CommentsREST
import entity.rest.gerrit.UserAccountGerrit
import extractor.ExtractorUtil
import extractor.gerrit.WriterCSV
import extractor.gerrit.WriterCSV.TypeCSV
import extractor.gerrit.WriterProvider
import loader.gerrit.LoaderChanges.Companion.baseUrlToDomain
import loader.gerrit.iterators.ChangeFilesValueIterator
import loader.gerrit.iterators.ChangesMetaDataFilesIterator
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


class GerritLoader(
  val baseUrl: String,
  val resultsDir: File,
  val patchsetCommentKey: String = DEFAULT_PATCHSET_COMMENT_KEY
) {

  companion object {
    const val DEFAULT_PATCHSET_COMMENT_KEY = "/PATCHSET_LEVEL"
    private val log = LoggerFactory.getLogger(GerritLoader::class.java)
  }

  private val datasetUrlDir =
    File(resultsDir, "dataset/gerrit/${baseUrlToDomain(baseUrl)}")
  private val writerProvider = WriterProvider()
  private val urlDir = File(resultsDir, "gerrit/${baseUrlToDomain(baseUrl)}")

  suspend fun run(
    ignoreLoad: Boolean = false,
    numOfThreads: Int = LoaderChanges.DEFAULT_NUM_THREADS
  ) {
    val loader = LoaderChanges(baseUrl, urlDir, numOfThreads = numOfThreads)
    if (!ignoreLoad) {
      loader.loadByIds()
    }
    datasetUrlDir.mkdirs()

    loader.processChanges { changeGerrit, comments ->
      addLineChanges(changeGerrit)
      addLineChangesFile(changeGerrit)
      addLineChangesReviewer(changeGerrit)
      addLineCommits(changeGerrit)
      addLineCommitsFile(changeGerrit)
      addLineCommitsAuthor(changeGerrit)
      addLineFiles(changeGerrit)
      comments?.let {
        addLinesComments(changeGerrit, it)
      }
      addUsers(changeGerrit)
    }

    writerProvider.writeAllAndClose()
  }

  private fun resultFile(key: String, project: String) = File(datasetUrlDir, "$key/${project}.csv")

  private fun getWriter(project: String, type: TypeCSV, file: File) = writerProvider.getWriter(project, type, file)

  private fun changesFile(project: String) = resultFile("changes", project)

  private fun String.addBackslashes() = this.replace("\n", "\\n").replace("\"", "\"\"")

  private fun addLineChanges(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = changesFile(project)
    val writer = getWriter(project, TypeCSV.CHANGES, file)

    val comments = mutableListOf<String>()
    changeGerrit.revisions.values.forEach { if (it.footer != null) comments.add(it.footer) }

    val comment = when {
      comments.size == 1 -> comments.first().addBackslashes()
      comments.size > 1 -> throw Exception("Gerrit change (id: ${changeGerrit.id}) contains more than one footers in revisions.")
      else -> {
        ""
      }
    }

    val subject = changeGerrit.subject.addBackslashes()

    writer.addLineCSV(
      changeGerrit.created,
      changeGerrit.number.toString(),
      changeGerrit.owner.keyUser,
      changeGerrit.status,
      addCommas(comment),
      changeGerrit.keyChange,
      changeGerrit.updated,
      addCommas(subject)
    )
  }

  private fun changesFileFile(project: String) = resultFile("changes_files", project)

  private fun addLineChangesFile(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = changesFileFile(project)
    val writer = getWriter(project, TypeCSV.CHANGES_FILES, file)

    val filePaths = changeGerrit.revisions.values.map { it.files.keys }.flatten().toSet()
    for (filePath in filePaths) {
      val keyFile = keyFile(changeGerrit, filePath)
      writer.addLineCSV(changeGerrit.keyChange, keyFile)
    }
  }

  private fun changesReviewerFile(project: String) = resultFile("changes_reviewer", project)

  private fun addLineChangesReviewer(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = changesReviewerFile(project)
    val writer = getWriter(project, TypeCSV.CHANGES_REVIEWER, file)

    val users = changeGerrit.reviewers.reviewers
    for (userProfile in users) {
      writer.addLineCSV(changeGerrit.keyChange, userProfile.keyUser)
    }
  }

  private fun commitsFile(project: String) = resultFile("commits", project)

  private fun addLineCommits(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = commitsFile(project)
    val writer = getWriter(project, TypeCSV.COMMITS, file)

    val dateFormatterGetter = ClientUtil.getDateFormatterGetter()
    val dateFormatterGithub = ClientUtil.getDateFormatterGithub()
    for ((hash, revision) in changeGerrit.revisions) {
      val committedDate = dateFormatterGithub.format(dateFormatterGetter.parse(revision.created))
      val keyCommit = keyCommit(project, hash)
      writer.addLineCSV(
        hash,
        committedDate,
        keyCommit,
        changeGerrit.keyChange
      )
    }
  }

  private fun commitsAuthorFile(project: String) = resultFile("commits_author", project)

  private fun addLineCommitsAuthor(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = commitsAuthorFile(changeGerrit.project)
    val writer = getWriter(project, TypeCSV.COMMITS_AUTHOR, file)

    for ((hash, revision) in changeGerrit.revisions) {
      val keyCommit = "${changeGerrit.project}:$hash"
      val author = revision.commit.author
      val committer = revision.commit.committer
      val uploader = revision.uploader
      writer.addLineCSV(keyCommit, author.keyUser, committer.keyUser, uploader.keyUser)
    }
  }

  private fun commitsFileFile(project: String) = resultFile("commits_file", project)

  private fun addLineCommitsFile(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = commitsFileFile(project)
    val writer = getWriter(project, TypeCSV.COMMITS_FILE, file)

    for ((hash, revision) in changeGerrit.revisions) {
      val keyCommit = keyCommit(project, hash)
      for ((filePath, fileGerrit) in revision.files) {
        val keyFile = keyFile(changeGerrit, filePath)
        writer.addLineCSV(
          keyCommit,
          keyFile,
          fileGerrit.linesInserted?.toString() ?: "",
          fileGerrit.linesDeleted?.toString() ?: "",
          fileGerrit.size?.toString() ?: "",
          fileGerrit.sizeDelta?.toString() ?: "",
          fileGerrit.status ?: "",
        )
      }
    }
  }

  private fun keyFile(changeGerrit: ChangeGerrit, filePath: String) = addCommas("${changeGerrit.project}:${filePath}")

  private fun keyCommit(project: String, hash: String) = "$project:$hash"
  private fun filesFile(project: String) = resultFile("files", project)

  private fun addLineFiles(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = filesFile(project)
    val writer = getWriter(project, TypeCSV.FILES, file)

    val filePaths = changeGerrit.revisions.values.map { it.files.keys }.flatten().toSet()
    for (filePath in filePaths) {
      val key = "${changeGerrit.project}:$filePath"
      writer.addLineCSV(filePath, key)
    }
  }

  private fun usersFile(project: String) = resultFile("users", project)

  private fun addUsers(changeGerrit: ChangeGerrit) {
    val project = changeGerrit.project
    val file = usersFile(project)
    val writer = getWriter(project, TypeCSV.USERS, file)

    changeGerrit.reviewers.reviewers.forEach {
      writer.addUser(it)
    }
    changeGerrit.revisions.values.map { writer.addUser(it.uploader) }
    writer.addUser(changeGerrit.owner)
  }

  private fun WriterCSV.addUser(user: UserAccountGerrit) =
    addLineCSV(
      addCommas(user.name),
      user.email,
      user.username
    )


  private fun commentsFileFile(project: String) = resultFile("comments_file", project)

  private fun commentsPatchFile(project: String) = resultFile("comments_patch", project)

  private fun addLinesComments(changeGerrit: ChangeGerrit, commentsREST: CommentsREST) {
    val project = changeGerrit.project
    val writerFiles = getWriter(project, TypeCSV.COMMENTS_FILE, commentsFileFile(project))
    val writerPatch = getWriter(project, TypeCSV.COMMENTS_PATCH, commentsPatchFile(project))

    val keyChange = changeGerrit.keyChange

    for ((key, comments) in commentsREST) {
      comments.forEach { comment ->
        val keyUser = comment.author.keyUser
        val time = comment.updated

        if (key != patchsetCommentKey) {
          writerFiles.addLineCSV(
            keyChange,
            keyUser,
            time,
            keyFile(changeGerrit, key)
          )
        } else {
          writerPatch.addLineCSV(
            keyChange,
            keyUser,
            time,
            comment.commitId
          )
        }
      }
    }
  }

  private fun addCommas(value: String) = "\"$value\""

  fun checkLoadedData() {
    val lightIds = loadLightChanges(urlDir)
    val loadedChanges = loadChanges(urlDir)
    val diff = lightIds - loadedChanges
    if (diff.isNotEmpty()) {
      log.info("Not loaded: ${diff.size}")
      log.info(diff.toString())
    }
  }

  private fun loadLightChanges(resultsDir: File): Set<Int> {
    val lightChangesIterator =
      ChangesMetaDataFilesIterator(ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "light_changes")))
    log.info("Checking loaded light changes.")
    return loadedChanges(lightChangesIterator)
  }

  private fun loadChanges(resultsDir: File): Set<Int> {
    val changesIterator =
      ChangeFilesValueIterator(ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "changes")))
    log.info("Checking loaded changes.")
    return loadedChanges(changesIterator)
  }

  private fun loadedChanges(changesIterator: Iterator<Change>): MutableSet<Int> {
    val dateFormatter = ClientUtil.getDateFormatterGetter()
    var minUpdated: Date? = null
    var maxUpdated: Date? = null
    val loadedChanges = mutableSetOf<Int>()
    var loadedChangesCopies = 0
    while (changesIterator.hasNext()) {
      val change = changesIterator.next()
      if (!loadedChanges.add(change.number)) loadedChangesCopies++
      val updatedDate = dateFormatter.parse(change.updated)
      if (minUpdated == null) minUpdated = updatedDate else {
        if (minUpdated > updatedDate) minUpdated = updatedDate
      }

      if (maxUpdated == null) maxUpdated = updatedDate else {
        if (maxUpdated < updatedDate) maxUpdated = updatedDate
      }
    }
    log.info("Loaded: ${loadedChanges.size}; Copies: $loadedChangesCopies; minUpdated: $minUpdated; maxUpdated: $maxUpdated")
    return loadedChanges
  }
}
