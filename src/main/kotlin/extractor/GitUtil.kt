package extractor

import entity.rest.github.GithubRepositoryInfo
import loader.gerrit.LoaderChanges.Companion.baseUrlToDomain
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.FS
import java.io.File

object GitUtil {
  fun isGitRepository(directory: File): Boolean {
    return RepositoryCache.FileKey.isGitRepository(directory, FS.DETECTED)
  }

  private fun getDiffsWithoutText(
    commit: RevCommit,
    reader: ObjectReader,
    git: Git
  ): List<DiffEntry> {
    val oldTreeIter = if (commit.parents.isNotEmpty()) {
      val firstParent = commit.parents[0]
      val treeParser = CanonicalTreeParser()
      treeParser.reset(reader, firstParent.tree)
      treeParser
    } else EmptyTreeIterator()

    val newTreeIter = CanonicalTreeParser()
    newTreeIter.reset(reader, commit.tree)

    return git.diff()
      .setNewTree(newTreeIter)
      .setOldTree(oldTreeIter)
      .setShowNameAndStatusOnly(true)
      .call()
  }

  private fun getFilePathAndId(diffEntry: DiffEntry): Pair<String, String> {
    return if (diffEntry.changeType == DiffEntry.ChangeType.DELETE) {
      diffEntry.oldPath to diffEntry.oldId.name()
    } else {
      diffEntry.newPath to diffEntry.newId.name()
    }
  }

  fun getChangedFiles(
    commit: RevCommit,
    reader: ObjectReader,
    git: Git
  ): Set<Pair<String, String>> {
    val result = mutableSetOf<Pair<String, String>>()
    val diffs = getDiffsWithoutText(commit, reader, git)

    for (entry in diffs) {
      result.add(getFilePathAndId(entry))
    }
    return result
  }

  fun getCommits(
    git: Git,
    repository: Repository,
    branchName: String,
    maxCount: Int? = null
  ): List<RevCommit> {
    val command = git.log().add(repository.resolve(branchName))
    maxCount?.let { command.setMaxCount(it) }
    return command.call().toList()
  }


  fun findMainBranch(git: Git): String {
    val allBranches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
    return allBranches.firstOrNull { it.name.isMainBranch() }?.name
      ?: allBranches.firstOrNull { it.name.isMasterBranch() }?.name
      ?: throw Throwable("Can't find main branch from: ${allBranches.map { it.name }}")
  }

  private fun String.isImportantBranch(branchName: String): Boolean {
    val prefixes = listOf(
      "",
      "refs/heads/",
      "origin/",
      "refs/remotes/origin/"
    )
    prefixes.forEach { if (this == "$it$branchName") return true }
    return false
  }

  private fun String.isMainBranch(): Boolean = isImportantBranch("main")

  private fun String.isMasterBranch(): Boolean = isImportantBranch("master")

  fun getDefaultRepositoryDir(githubRepositoryInfo: GithubRepositoryInfo) =
    File("./repositories/${githubRepositoryInfo.owner}/${githubRepositoryInfo.name}")

  fun getDefaultRepositoryDir(baseUrl: String) = File("./repositories/${baseUrlToDomain(baseUrl)}/")

}