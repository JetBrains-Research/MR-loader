package extractor.github

import client.ClientUtil
import entity.rest.github.GithubRepositoryInfo
import entity.rest.github.UserProfile
import extractor.ExtractorUtil.lineCSV
import extractor.GitUtil.findMainBranch
import extractor.GitUtil.getChangedFiles
import extractor.GitUtil.getCommits
import extractor.GitUtil.getDefaultRepositoryDir
import extractor.GitUtil.isGitRepository
import loader.github.LoaderPR
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import java.util.*

class ExtractorPR(
  private val githubRepositoryInfo: GithubRepositoryInfo,
  private val repositoryDir: File = getDefaultRepositoryDir(githubRepositoryInfo),
  token: String = githubToken
) {
  companion object {
    const val COMMIT_AUTHOR_FILENAME = "commit_author.csv"
    const val COMMIT_FILE_FILENAME = "commit_file.csv"
    const val COMMIT_FILENAME = "commit.csv"
    const val PARTICIPANT_FILENAME = "participant.csv"
    const val PULL_AUTHOR_FILENAME = "pull_author.csv"
    const val PULL_FILE_FILENAME = "pull_file.csv"
    const val PULL_FILENAME = "pull.csv"
    const val REVIEWER_FILENAME = "reviewer.csv"
    const val GIT_USERS_FILENAME = "git_users.csv"
    const val GITHUB_USERS_FILENAME = "github_users.csv"

    private val versionProperties = Properties()

    private val githubToken: String
      get() {
        return versionProperties.getProperty("githubToken") ?: throw Exception("Can't find GitHub token.")
      }

    init {
      val versionPropertiesFile = this.javaClass.getResourceAsStream("/generated.properties")
      versionProperties.load(versionPropertiesFile)
    }
  }

  private val loader = LoaderPR(githubRepositoryInfo, token)
  private val gitDir = File(repositoryDir, ".git")
  private val resultsFolder = File("./dataset/github/${githubRepositoryInfo.owner}/${githubRepositoryInfo.name}")
  private val dateFormatter = ClientUtil.getDateFormatterGithub()
  private val resultFiles = HashMap<String, File>()

  suspend fun run(
    waitRateLimit: Boolean = true,
    ignoreLoad: Boolean = false,
    branchName: String? = null,
    upTo: Date? = null
  ) {
    if (resultsFolder.exists()) resultsFolder.deleteRecursively()
    resultsFolder.mkdirs()
    repositoryDir.mkdirs()

    if (!ignoreLoad)
      loader.load(
        upTo = upTo,
        waitRateLimit = waitRateLimit
      )

    loadRepository()
    val repository = FileRepository(gitDir)
    val git = Git(repository)
    val reader = repository.newObjectReader()
    val mainBranch = branchName ?: findMainBranch(git)
    val commits = getCommits(git, repository, mainBranch).associateBy { it.name }
    val githubProfiles = mutableSetOf<UserProfile>()
    val gitProfiles = mutableSetOf<Pair<String, String>>()
    addHeaders()
    loader.processPRs { prInfo ->
      val pullNumber = prInfo.number.toString()
      println("Processing $pullNumber")
      prInfo.reviewers.forEach {
        addLineParticipant(pullNumber, it.login)
        addLineReviewer(pullNumber, it.login)
      }
      githubProfiles.addAll(prInfo.reviewers)
      addLinePull(pullNumber, prInfo.closedAt)
      addLinePullAuthor(pullNumber, prInfo.author.login)

      prInfo.commits.forEach { sha ->
        commits[sha]?.let { commit ->
          addLineCommitAuthor(commit)
          addLineCommit(commit)

          getChangedFiles(commit, reader, git).forEach {
            val (filePath, oid) = it
            addLineCommitFile(oid, filePath)
            addLinePullFile(pullNumber, filePath)
          }

          gitProfiles.add(commit.authorIdent.name to commit.authorIdent.emailAddress)
        }
      }
    }

    githubProfiles.forEach { addLineGithubUsers(it) }
    gitProfiles.forEach { addLineGitUsers(it) }
  }

  private fun loadRepository() {
    println("Checking local repository")
    if (isGitRepository(gitDir)) return

    println("Repository needs to be loaded")
    val repoURI = "https://github.com/${githubRepositoryInfo.owner}/${githubRepositoryInfo.name}.git"
    Git.cloneRepository()
      .setURI(repoURI)
      .setDirectory(repositoryDir)
      .setNoCheckout(true)
      .call().use { result ->
        println("Finish loading repo $repoURI")
        println("Repository inside: " + result.repository.directory)
      }
  }

  private fun addLineToFile(fileName: String, vararg fields: String) {
    val file = resultFiles.computeIfAbsent(fileName) { File(resultsFolder, fileName) }
    file.appendText(lineCSV(*fields))
  }

  private fun addHeaders() {
    addLineCommitAuthor("oid", "git_email")
    addLineCommitFile("oid", "file_path")
    addLineParticipant("pull_number", "participant_login")
    addLinePullAuthor("pull_number", "author_login")
    addLinePullFile("pull_number", "author_login")
    addLinePull("pull_number", "closed_at")
    addLineReviewer("pull_number", "reviewer_login")
    addLineGitUsers("git_name", "git_email")
    addLineGithubUsers("github_login", "github_name", "github_email")
  }

  // TODO: remake to login
  private fun addLineCommitAuthor(name: String, email: String) =
    addLineToFile(COMMIT_AUTHOR_FILENAME, name, email)

  private fun addLineCommitAuthor(commit: RevCommit) =
    addLineCommitAuthor(commit.name, commit.authorIdent.emailAddress)

  private fun addLineCommitFile(oid: String, filePath: String) =
    addLineToFile(COMMIT_FILE_FILENAME, oid, filePath)

  private fun addLineCommit(sha: String, date: String) =
    addLineToFile(COMMIT_FILENAME, sha, date)

  private fun addLineCommit(commit: RevCommit) {
    val date = Date(commit.commitTime * 1000L)
    addLineCommit(commit.name, dateFormatter.format(date))
  }

  private fun addLineParticipant(pullNumber: String, participantLogin: String) =
    addLineToFile(PARTICIPANT_FILENAME, pullNumber, participantLogin)

  private fun addLinePullAuthor(pullNumber: String, login: String) =
    addLineToFile(PULL_AUTHOR_FILENAME, pullNumber, login)

  private fun addLinePullFile(pullNumber: String, filePath: String) =
    addLineToFile(PULL_FILE_FILENAME, pullNumber, filePath)

  private fun addLinePull(pullNumber: String, closedAt: String) =
    addLineToFile(PULL_FILENAME, pullNumber, closedAt)

  private fun addLineReviewer(pullNumber: String, login: String) =
    addLineToFile(REVIEWER_FILENAME, pullNumber, login)

  private fun addLineGithubUsers(login: String, name: String, email: String) =
    addLineToFile(GITHUB_USERS_FILENAME, login, name, email)

  private fun addLineGithubUsers(userProfile: UserProfile) =
    addLineGithubUsers(userProfile.login, userProfile.name, userProfile.email)

  private fun addLineGitUsers(name: String, email: String) =
    addLineToFile(GIT_USERS_FILENAME, name, email)

  private fun addLineGitUsers(nameToEmail: Pair<String, String>) =
    addLineGitUsers(nameToEmail.first, nameToEmail.second)
}