import client.ClientGerritREST
import client.ClientUtil
import extractor.ExtractorUtil
import extractor.gerrit.ExtractorChanges
import loader.gerrit.LoaderChanges
import java.io.File
import java.util.*


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
  val lightIds = loadLightChanges(resultsDir)
  val loadedChanges = loadChanges(resultsDir)
  val diff = lightIds - loadedChanges
  println("Not loaded: ${diff.size}")
  println(diff)
}

fun loadLightChanges(resultsDir: File): Set<Int> {
  val json = ClientGerritREST().json

  val lightChangesIterator =
    LoaderChanges.ChangesMetaDataFilesIterator(
      ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "light_changes")),
      json
    )

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

  println("$resultsDir : Light: ${lightIds.size}; Copies: $lightIdsCopies; minUpdated: $minUpdatedLight; maxUpdated: $maxUpdatedLight")

  return lightIds
}

fun loadChanges(resultsDir: File): Set<Int> {
  val json = ClientGerritREST().json
  val changesIterator =
    LoaderChanges.ChangeFilesValueIterator(ExtractorUtil.getFilesIgnoreHidden(File(resultsDir, "changes")), json)

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
  println("Loaded: ${loadedChanges.size}; Copies: $loadedChangesCopies; minUpdated: $minUpdated; maxUpdated: $maxUpdated")
  return loadedChanges
}

suspend fun main() {
  loadGerrit()
}