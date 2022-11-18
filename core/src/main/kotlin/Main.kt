import client.ClientGerritREST
import client.ClientUtil
import entity.rest.github.GithubRepositoryInfo
import extractor.ExtractorUtil
import extractor.gerrit.ExtractorChanges
import extractor.github.ExtractorPR
import loader.gerrit.LoaderChanges
import java.io.File
import java.util.*


suspend fun loadGithub() {
  val reposInfos = listOf(
    GithubRepositoryInfo("apache", "beam") to null,
    GithubRepositoryInfo("apache", "flink") to null,
    GithubRepositoryInfo("apache", "kafka") to "refs/remotes/origin/trunk",
    GithubRepositoryInfo("apache", "spark") to null,
    GithubRepositoryInfo("apache", "zookeeper") to null,
  )

  val dateFormat = ClientUtil.getDateFormatterGithub()
  val upTo = dateFormat.parse("2020-09-02T00:00:00Z")
  for ((githubRepositoryInfo, branchName) in reposInfos) {
    val extractorPR = ExtractorPR(githubRepositoryInfo)
    extractorPR.run(upTo = upTo, branchName = branchName)
  }
}

suspend fun loadGerrit() {
  val dateFormat = ClientUtil.getDateFormatterGetter()

//  "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
  val beforeThreshold = dateFormat.parse("2022-10-21 23:59:00.000000000")
  val afterThreshold = dateFormat.parse("2022-09-30 23:59:00.000000000")

  val path = System.getProperty("user.home") + File.separator + "work"
  val dir = File(path)
  val dataDir = File(dir, "__openstack")
  val url = "http://review.openstack.org"
  val patchsetCommentKey = "/PATCHSET_LEVEL"
  val extractorChanges =
    ExtractorChanges(url, dataDir, patchsetCommentKey = patchsetCommentKey)
  extractorChanges.run(beforeThreshold = beforeThreshold, afterThreshold = afterThreshold)

  val resultsDir = File(dataDir, LoaderChanges.baseUrlToDomain(url))
  check(resultsDir)

}

fun check(resultsDir: File) {
  val json = ClientGerritREST().json

  val lightChangesIterator =
    LoaderChanges.ChangesMetaDataFilesIterator(
      ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "light_changes")),
      json
    )
  val changesIterator =
    LoaderChanges.ChangeFilesValueIterator(ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "changes")), json)

  val dateFormatter = ClientUtil.getDateFormatterGetter()

  var minUpdatedLight: Date? = null
  var maxUpdatedLight: Date? = null
  val lightIds = mutableSetOf<Int>()
  var lightIdsCopies = 0
  while (lightChangesIterator.hasNext()) {
    val changeMetaData = lightChangesIterator.next()
    if (!lightIds.add(changeMetaData.number)) lightIdsCopies++
    val updatedDate = dateFormatter.parse(changeMetaData.updated)
    if (minUpdatedLight == null) minUpdatedLight = updatedDate else {
      if (minUpdatedLight > updatedDate) minUpdatedLight = updatedDate
    }

    if (maxUpdatedLight == null) maxUpdatedLight = updatedDate else {
      if (maxUpdatedLight < updatedDate) maxUpdatedLight = updatedDate
    }
  }

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

  val diff = lightIds - loadedChanges

  println("Light: ${lightIds.size}; Copies: $lightIdsCopies; minUpdated: $minUpdatedLight; maxUpdated: $maxUpdatedLight")
  println("Loaded: ${loadedChanges.size}; Copies: $loadedChangesCopies; minUpdated: $minUpdated; maxUpdated: $maxUpdated")
  println("Not loaded: ${diff.size}")
  println(diff)
}

suspend fun main() {
//  loadGithub()
  loadGerrit()
//  checkSlowLoad()
//  checkLoadedSingleChangeMod()
//  checkLoadedLight()
}