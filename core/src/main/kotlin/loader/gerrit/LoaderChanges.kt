package loader.gerrit

import client.ClientGerritREST
import client.ClientUtil
import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.ChangeMetaData
import entity.rest.gerrit.CommentsREST
import extractor.ExtractorUtil.getFilesIgnoreHidden
import extractor.gerrit.JsonObjectsListBuffer
import extractor.gerrit.JsonObjectsMapBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter


class LoaderChanges(
  val baseUrl: String,
  resultDir: File? = null,
  val beforeThreshold: Date? = null,
  val afterThreshold: Date? = null,
  val ignoreState: Boolean = false
) {

  companion object {
    const val TIMEOUT = 500L

    // TODO: needs check
    private val IGNORE_PROJECTS = setOf("All-Projects", "All-Users")

    fun baseUrlToDomain(baseUrl: String) = baseUrl
      .removePrefix("http://")
      .removePrefix("https://")
      .removeSuffix("/")

    private val logger = run {
      val result = Logger.getLogger("loader")
      val fh = FileHandler("./loader.log")
      result.addHandler(fh)
      val formatter = SimpleFormatter()
      fh.formatter = formatter
      result
    }
  }

  class ChangeFilesValueIterator(files: Sequence<File>, private val json: Json) : Iterator<ChangeGerrit> {
    private var fileIter = files.iterator()
    private var valueIter: Iterator<ChangeGerrit>? = null

    override fun next(): ChangeGerrit {
      return valueIter!!.next()
    }

    override fun hasNext(): Boolean {
      checkAndSetIterators()
      return !valueIteratorIsFinished()
    }

    private fun checkAndSetIterators() {
      if (valueIteratorIsFinished()) {
        if (fileIter.hasNext()) {
          val file = fileIter.next()
          valueIter = json.decodeFromString<Map<Int, ChangeGerrit>>(file.readText()).values.iterator()
        }
      }
    }

    private fun valueIteratorIsFinished() = valueIter == null || valueIter?.hasNext() == false

  }

  class ChangesMetaDataFilesIterator(files: Sequence<File>, private val json: Json) : Iterator<ChangeMetaData> {
    private var fileIter = files.iterator()
    private var listIter: Iterator<List<ChangeMetaData>>? = null
    private var valueIter: Iterator<ChangeMetaData>? = null

    override fun next(): ChangeMetaData {
      return valueIter!!.next()
    }

    override fun hasNext(): Boolean = checkAndSetIterators()


    private fun checkAndSetIterators(): Boolean {
      when {
        listIter == null -> resetIterators()

        valueIter?.hasNext() == false -> {
          if (listIter!!.hasNext()) {
            resetValueIterator()
          } else {
            resetIterators()
          }
        }
      }

      return valueIter?.hasNext() ?: false
    }

    private fun resetIterators() {
      if (fileIter.hasNext()) {
        val file = fileIter.next()
        val listOfList = json.decodeFromString<List<List<ChangeMetaData>>>(file.readText())
        // assume files not empty
        listIter = listOfList.iterator()
        resetValueIterator()
      }
    }

    private fun resetValueIterator() {
      val list = listIter!!.next()
      valueIter = list.iterator()
    }
  }

  @Serializable
  private data class Parameters(
    val baseUrl: String,
    val beforeThreshold: String? = null,
    val afterThreshold: String? = null
  )

  @Serializable
  private data class StateOfLoad(
    val parameters: Parameters,
    var finishedLoadingIds: Boolean = false,
    var finishedLoadingChanges: Boolean = false,
    var finishedLoadingComments: Boolean = false
  )

  private val resultsDir = run {
    val folderName = baseUrlToDomain(baseUrl)
    if (resultDir == null) File("./GerritResults/$folderName") else File(resultDir, folderName)
  }
  private val changesDir = File(resultsDir, "changes")
  private val commentsDir = File(resultsDir, "comments")
  private val lightChangesDir = File(resultsDir, "light_changes")
  private val errorsDir = File(resultsDir, "errors")
  private val errorsChangesDir = File(errorsDir, "changes")
  private val errorsCommentsDir = File(errorsDir, "comments")
  private val stateOfLoadFile = File(resultDir, "state")


  private val client = ClientGerritREST()
  private val dateFormatter = ClientUtil.getDateFormatterGetter()
  private val errorsChangesBuffer = JsonObjectsMapBuffer(errorsChangesDir)
  private val errorsCommentsBuffer = JsonObjectsMapBuffer(errorsCommentsDir)

  private val series = arrayOf(300, 150, 100, 50, 30, 15, 10, 5, 3, 1)
  private val nChangesAtomic = AtomicInteger(series[0])

  private val stateOfLoad = run {
    val default = StateOfLoad(
      Parameters(
        baseUrl,
        beforeThreshold?.let {
          dateFormatter.format(it)
        },
        afterThreshold?.let {
          dateFormatter.format(it)
        }
      ))
    return@run if (stateOfLoadFile.exists()) {
      val prevState = client.json.decodeFromString<StateOfLoad>(stateOfLoadFile.readText())
      if (prevState.parameters != default.parameters) default else prevState
    } else default

  }

  suspend fun loadByIds() {
    changesDir.mkdirs()
    commentsDir.mkdirs()
    lightChangesDir.mkdirs()
    errorsChangesDir.mkdirs()
    errorsCommentsDir.mkdirs()
    saveState()

    loadNeededIds()

    val nThreads = 12
    val threadPool = Executors.newFixedThreadPool(nThreads)

    try {
      loadChangesByIds(threadPool, nThreads)
      loadComments(threadPool, nThreads)
    } finally {
      threadPool.shutdown()
    }


    errorsChangesBuffer.close()
    errorsCommentsBuffer.close()
  }

  private suspend fun loadNeededIds() {
    if (stateOfLoad.finishedLoadingIds && !ignoreState) return

    val projects = client.getProjects(baseUrl)
    logger.info("Number of projects: ${projects.size}")
    val jsonObjectsListBuffer = JsonObjectsListBuffer(lightChangesDir)

    for (project in projects) {
      if (project in IGNORE_PROJECTS) continue
      var moreChanges = true
      var before = beforeThreshold?.let { dateFormatter.format(beforeThreshold) }
      while (moreChanges) {
        Thread.sleep(TIMEOUT)
        val rawJson =
          getChangesWrapped(project, before = before, light = true)
        val metaData = decodeChangesMetaData(rawJson)

        if (metaData.isEmpty()) {
          logger.warning("Got empty list of changes for project=$project; before=$before;")
          break
        }

        val lastChange = metaData.last()
        if (afterThreshold != null) {
          val lastDate = dateFormatter.parse(lastChange.updated)
          if (lastDate <= afterThreshold) {
            val firstChange = metaData.first()
            val firstDate = dateFormatter.parse(firstChange.updated)
            if (firstDate <= afterThreshold) {
              logger.info("Skipping load for project=$project; before=$before; size=${metaData.size}; not in threshold firstDate=$firstDate; lastDate=$lastDate")
              break
            }
            jsonObjectsListBuffer.addObject(rawJson)
            logger.info("Loaded changes for project=$project; firstDate=$firstDate; lastDate=$lastDate size=${metaData.size};")
            break
          }
        }
        jsonObjectsListBuffer.addObject(rawJson)
        before = lastChange.updated
        moreChanges = lastChange.moreChanges ?: false

        logger.info("Loaded changes for project=$project; before=$before; size=${metaData.size}; moreChanges=$moreChanges")
      }
    }

    jsonObjectsListBuffer.close()
    stateOfLoad.finishedLoadingIds = true
    saveState()
    logger.info("Finished light load of changes.")
  }


  private suspend fun loadChangesByIds(threadPool: ExecutorService, nThreads: Int) {
    if (stateOfLoad.finishedLoadingChanges && !ignoreState) return

    val futures = mutableListOf<Future<Boolean>>()
    val jsonObjectBuffer = JsonObjectsMapBuffer(changesDir)
    val iter = ChangesMetaDataFilesIterator(getFilesIgnoreHidden(lightChangesDir), client.json)
    val loadedIds = if (ignoreState) null else loadedChangeIds()
    addLoadSingleChange(futures, iter, jsonObjectBuffer, threadPool, nThreads, loadedIds)
    try {
      wrapThreadPool(futures) {
        addLoadSingleChange(futures, iter, jsonObjectBuffer, threadPool, it.size, loadedIds)
      }
    } finally {
      jsonObjectBuffer.close()
    }
    stateOfLoad.finishedLoadingChanges = true
    saveState()
    logger.info("Finished loading changes by id.")
  }

  private fun loadedChangeIds(): Set<Int> {
    val ids = mutableSetOf<Int>()
    val changeFilesValueIterator = ChangeFilesValueIterator(loadedChangeFiles(), client.json)
    while (changeFilesValueIterator.hasNext()) {
      val change = changeFilesValueIterator.next()
      ids.add(change.number)
    }
    return ids
  }

  private fun addLoadSingleChange(
    futures: MutableList<Future<Boolean>>,
    idsIterator: ChangesMetaDataFilesIterator,
    jsonObjectBuffer: JsonObjectsMapBuffer,
    threadPool: ExecutorService,
    numOfTasks: Int,
    loadedIds: Set<Int>? = null
  ) {
    var count = 0
    while (count != numOfTasks) {
      if (idsIterator.hasNext()) {
        val metaData = idsIterator.next()
        val id = metaData.number

        if (loadedIds != null && id in loadedIds) {
          logger.info("Already got change: $id. Skipping.")
          continue
        }

        if (!changeInThresholds(metaData)) {
          logger.info("Change $id not in thresholds. Skipping.")
          continue
        }

        val callable = Callable {
          runBlocking {
            wrapIgnoringErrors { client.getChangeRaw(baseUrl, id) }?.let { rawJson ->
              jsonObjectBuffer.addEntry(rawJson, id)
            }
            logger.info("Loaded changes for $id")
            true
          }
        }
        futures.add(threadPool.submit(callable))
      } else break
      count++
    }
    logger.info("Added $count change loading tasks.")
  }

  private fun changeInThresholds(change: ChangeMetaData): Boolean {
    var result = true
    val date = dateFormatter.parse(change.updated)
    beforeThreshold?.let {
      if (date >= it) result = false
    }
    afterThreshold?.let {
      if (date <= it) result = false
    }
    return result
  }

  // TODO: rewrite it, to sorted order comments loading with mutable maps inside. In this case there will be max 2 maps.
  private fun commentsMap(): HashMap<Int, CommentsREST> {
    val files = loadedCommentsFiles()
    val map = HashMap<Int, CommentsREST>()
    for (file in files) {
      val comments = decodeComments(file.readText())
      map.putAll(comments)
    }
    return map
  }

  fun processChanges(processChanges: (ChangeGerrit, CommentsREST?) -> Unit) {
    logger.warning("Start processing changes.")
    val files = loadedChangeFiles()
    val commentsMap = commentsMap()
    val changeIds = mutableSetOf<Int>()
    for (file in files) {
      val changesMap = decodeRawJson<Map<Int, ChangeGerrit>>(file.readText())
      for ((id, change) in changesMap) {
        val changeId = change.number

        if (id != change.number) throw Exception("request id: $id != loaded id : $changeId")

        if (beforeThreshold != null) {
          val date = dateFormatter.parse(change.updated)
          if (date > beforeThreshold) {
            logger.warning("Change $changeId > beforeThreshold")
            continue
          }
        }

        if (afterThreshold != null) {
          val date = dateFormatter.parse(change.updated)
          if (date < afterThreshold) {
            logger.warning("Change $changeId > afterThreshold")
            continue
          }
        }

        if (changeId in changeIds) {
          logger.warning("Got copy change $changeId")
          continue
        }
        changeIds.add(changeId)

        val comments = commentsMap[changeId]
        if (change.totalCommentCount > 0 && comments == null) throw Exception("Can't find comments for ${change.number}")
        processChanges(change, comments)
      }
      logger.info("Finished changes from ${file.name} file")
    }
  }

  // TODO: refactor
  private fun decreaseNumOfChanges(prevValue: Int) {
    // 300, 150, 100, 50, 30, 15, 10, 5, 3, 1
    val idx = series.indexOfFirst { it == prevValue }
    if (idx + 1 >= series.size) throw Exception("Can't load less then ${series[idx]}")
    nChangesAtomic.compareAndSet(prevValue, series[idx + 1])
  }

  //TODO: Refactor
  private suspend fun getChangesWrapped(
    project: String,
    before: String? = null,
    after: String? = null,
    maxNumOfErrors: Int = 5,
    light: Boolean = false
  ): String {
    var numOfErrors = 0
    while (true) {
      val nChanges = nChangesAtomic.get()
      val msg = "Raw changes before=$before; after=$after; nChanges=$nChanges"
      try {
        return if (!light) client.getChangesRaw(
          baseUrl,
          before,
          after,
          nChanges
        ) else client.getChangesRawLightWithProject(baseUrl, project, before, after, nChanges)
      } catch (e: Throwable) {

        if (e.message?.contains("408") == true) {
          logger.warning("Decreasing number of changes per request. Previous: $nChanges. : Got error ${e.message}")
          decreaseNumOfChanges(nChanges)
          continue
        }

        if (e.message?.contains("429") == true) {
          logger.warning("$msg : Error message contains 429 going sleep for 1 minute.")
          Thread.sleep(60_000)
          continue
        }

        logger.severe("$msg : ${e.message}")
        numOfErrors += 1
        if (numOfErrors > maxNumOfErrors) {
          throw e
        }
      }
    }
  }

  private suspend fun <T> wrapIgnoringErrors(
    msg: String = "",
    maxNumOfErrors: Int = 3,
    proceedError: (Throwable) -> Unit = {},
    task: suspend () -> T?
  ): T? {
    var numOfErrors = 0
    while (true) {
      try {
        return task()
      } catch (e: Throwable) {
        if (e.message?.contains("429") == true) {
          logger.warning("$msg : Error message contains 429 going sleep for 1 minute.")
          Thread.sleep(60_000)
          continue
        }

        logger.severe("$msg : ${e.message}")
        numOfErrors += 1
        if (numOfErrors > maxNumOfErrors) {
          proceedError(e)
          return null
        }
      }
    }
  }

  private fun loadedChangeFiles() = getFilesIgnoreHidden(changesDir)

  private fun loadedCommentsFiles() = getFilesIgnoreHidden(commentsDir)

  private fun decodeChangesMetaData(rawJson: String) = decodeRawJson<List<ChangeMetaData>>(rawJson)

  private inline fun <reified T> decodeRawJson(rawJson: String) = client.json.decodeFromString<T>(rawJson)

  private fun decodeComments(rawJson: String) = decodeRawJson<Map<Int, CommentsREST>>(rawJson)

  private fun loadedComments(): MutableSet<Int> {
    val commentIds = mutableSetOf<Int>()
    val files = loadedCommentsFiles()
    for (file in files) {
      val comments = decodeComments(file.readText())
      val ids = comments.map { it.key }
      commentIds.addAll(ids)
    }
    return commentIds
  }

  private suspend fun loadComments(threadPool: ExecutorService, nThreads: Int) {
    if (stateOfLoad.finishedLoadingComments && !ignoreState) return

    val loadedChangesFiles = loadedChangeFiles()
    val commentIds = loadedComments()

    val jsonObjectBuffer = JsonObjectsMapBuffer(commentsDir)

    val changeFilesValueIterator = ChangeFilesValueIterator(loadedChangesFiles, client.json)
    val futures = mutableListOf<Future<Boolean>>()
    addCommentLoadTasks(futures, threadPool, changeFilesValueIterator, commentIds, jsonObjectBuffer, nThreads)


    wrapThreadPool(futures, delay = 500) {
      addCommentLoadTasks(futures, threadPool, changeFilesValueIterator, commentIds, jsonObjectBuffer, it.size)
    }

    jsonObjectBuffer.close()

    stateOfLoad.finishedLoadingComments = true
    saveState()
    logger.info("Finished loading comments.")
  }

  private fun addCommentLoadTasks(
    futures: MutableList<Future<Boolean>>,
    threadPool: ExecutorService,
    changeFilesValueIterator: ChangeFilesValueIterator,
    commentIds: MutableSet<Int>,
    jsonObjectBuffer: JsonObjectsMapBuffer,
    amount: Int
  ) {
    var count = 0
    while (count != amount) {
      if (!changeFilesValueIterator.hasNext()) break
      val change = changeFilesValueIterator.next()
      val id = change.number
      if (change.totalCommentCount == 0 || id in commentIds) continue

      commentIds.add(id)

      val runnable = Callable {
        runBlocking {
          try {
            val rawComments = wrapIgnoringErrors("Raw comments changeId=${id}", proceedError = {
              errorsCommentsBuffer.addEntry(it.message ?: "", id)
            }) {
              client.getCommentsRaw(
                baseUrl,
                changeId = id
              )
            }
            rawComments?.let { jsonObjectBuffer.addEntry(it, id) }

            logger.info("Loaded comments for ${id}")
          } catch (e: Throwable) {
            errorsCommentsBuffer.addEntry(e.message ?: "", id)
          }

          true
        }
      }
      futures.add(threadPool.submit(runnable))
      count++
    }

  }

  private suspend inline fun <T> wrapThreadPool(
    futures: MutableList<Future<T>>,
    delay: Long = TIMEOUT,
    processResults: (List<T>) -> Unit
  ) {
    while (futures.isNotEmpty()) {
      val iter = futures.listIterator()
      val results = mutableListOf<T>()
      while (iter.hasNext()) {
        val future = iter.next()
        val isFinished = future.isDone || future.isCancelled
        if (!isFinished) continue

        val loadTaskData = try {
          withContext(Dispatchers.IO) {
            val result = future.get()
            iter.remove()
            result
          }
        } catch (e: Throwable) {
          logger.severe("Getting error from thread pool: ${e.message}")
          null
        }
        loadTaskData?.let { results.add(it) }
      }

      if (results.isNotEmpty())
        processResults(results)

      Thread.sleep(delay)
    }
  }

  private fun saveState() = stateOfLoadFile.writeText(client.json.encodeToString(stateOfLoad))
}
