package loader.gerrit

import client.ClientGerritREST
import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.ChangeMetaData
import entity.rest.gerrit.CommentsREST
import extractor.ExtractorUtil.getFilesIgnoreHidden
import extractor.gerrit.JsonObjectsMapBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import loader.gerrit.iterators.ChangeFilesValueIterator
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

@Serializable
private data class Parameters(
  val baseUrl: String
)

@Serializable
private data class StateOfLoad(
  val parameters: Parameters,
  var finishedLoadingChanges: Boolean = false,
  var finishedLoadingComments: Boolean = false
)

class LoaderChanges(
  val baseUrl: String,
  resultDir: File? = null,
  val ignoreState: Boolean = false,
  val numOfThreads: Int = DEFAULT_NUM_THREADS
) {

  companion object {
    const val TIMEOUT = 500L
    const val DEFAULT_NUM_THREADS = 12

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

  private val stateOfLoad = run {
    val default = StateOfLoad(
      Parameters(
        baseUrl
      )
    )
    return@run if (stateOfLoadFile.exists()) {
//      val prevState = json.decodeFromString<StateOfLoad>(stateOfLoadFile.readText())
//      if (prevState.parameters != default.parameters) default else prevState
      default
    } else default

  }

  private val json = ClientGerritREST.json

  suspend fun loadByIds() {
    changesDir.mkdirs()
    commentsDir.mkdirs()
    lightChangesDir.mkdirs()
    errorsChangesDir.mkdirs()
    errorsCommentsDir.mkdirs()
    saveState()

//    val maxChange = findMax()?.number ?: throw Throwable("There is no max change.")
    val maxChange = 1000

    val threadPool = Executors.newFixedThreadPool(numOfThreads)

    try {
      loadChangesByIds(maxChange, threadPool, numOfThreads)
      loadComments(threadPool, numOfThreads)
    } finally {
      threadPool.shutdown()
    }
  }

  private suspend fun findMax(): ChangeMetaData? {
    val rawBatch = wrapIgnoringErrors(msg = "Loading first batch of light changes url=$baseUrl") {
      client.getChangesRawLightNew(
        baseUrl,
        0
      )
    } ?: return null
    val batch = decodeChangesMetaData(rawBatch)
    return batch.maxByOrNull { it.number }
  }

  private suspend fun loadChangesByIds(maxId: Int, threadPool: ExecutorService, nThreads: Int) {
    if (stateOfLoad.finishedLoadingChanges && !ignoreState) return

    val futures = mutableListOf<Future<Boolean>>()
    val jsonObjectBuffer = JsonObjectsMapBuffer(changesDir)
    val iter = (0..maxId).iterator()
    val loadedIds = if (ignoreState) null else loadedChangeIds()
    try {
      while (iter.hasNext()) {
        addLoadSingleChange(futures, iter, jsonObjectBuffer, threadPool, nThreads, loadedIds)
        wrapThreadPool(futures) {}
      }
      wrapThreadPool(futures) {}
    } finally {
      jsonObjectBuffer.close()
    }
    stateOfLoad.finishedLoadingChanges = true
    saveState()
    logger.info("Finished loading changes by id.")
  }

  private fun loadedChangeIds(): Set<Int> {
    val ids = mutableSetOf<Int>()
    val changeFilesValueIterator = ChangeFilesValueIterator(loadedChangeFiles())
    while (changeFilesValueIterator.hasNext()) {
      val change = changeFilesValueIterator.next()
      ids.add(change.number)
    }
    return ids
  }

  private fun addLoadSingleChange(
    futures: MutableList<Future<Boolean>>,
    idsIterator: Iterator<Int>,
    jsonObjectBuffer: JsonObjectsMapBuffer,
    threadPool: ExecutorService,
    numOfTasks: Int,
    loadedIds: Set<Int>? = null
  ) {
    var count = 0
    while (count != numOfTasks) {
      if (idsIterator.hasNext()) {
        val id = idsIterator.next()

        if (loadedIds != null && id in loadedIds) {
          logger.info("Already got change: $id. Skipping.")
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

  fun processChanges(processChanges: (ChangeGerrit, CommentsREST?) -> Unit) {
    logger.warning("Start processing changes.")
    val files = loadedChangeFiles()
    val commentsLoader = CommentsLoader(loadedCommentsFiles())
    val changeIds = mutableSetOf<Int>()
    for (file in files) {
      val changesMap = decodeRawJson<Map<Int, ChangeGerrit>>(file.readText())
      for ((id, change) in changesMap) {
        val changeId = change.number

        if (id != change.number) throw Exception("request id: $id != loaded id : $changeId")

        if (changeId in changeIds) {
          logger.warning("Got copy change $changeId")
          continue
        }
        changeIds.add(changeId)

        val comments = commentsLoader.get(changeId)
        if (change.totalCommentCount > 0) throw Exception("Can't find comments for ${change.number}")
        processChanges(change, comments)
      }
      logger.info("Finished changes from ${file.name} file")
    }
  }

  private suspend fun <T> wrapIgnoringErrors(
    msg: String = "",
    maxNumOfErrors: Int = 3,
    task: suspend () -> T?
  ): T? {
    var numOfErrors = 1
    while (true) {
      try {
        return task()
      } catch (e: Throwable) {
        val errMsg = e.message ?: "Error message is empty"
        when {
          errMsg.contains("404") -> {
            logger.warning("$msg : Skipping. Error message contains 404.")
            return null
          }

          errMsg.contains("429") -> {
            logger.warning("$msg : Sleep for 1 minute. Error message contains 429.")
            Thread.sleep(60_000)
            continue
          }

          numOfErrors > maxNumOfErrors -> {
            logger.severe("Number of errors for task exceeded threshold=$maxNumOfErrors : $msg : $errMsg")
            return null
          }
        }

        numOfErrors += 1
        logger.warning("Got error : $msg : $errMsg")
      }
    }
  }

  private fun loadedChangeFiles() = getFilesIgnoreHidden(changesDir)

  private fun loadedCommentsFiles() = getFilesIgnoreHidden(commentsDir)

  private fun decodeChangesMetaData(rawJson: String) = decodeRawJson<List<ChangeMetaData>>(rawJson)

  private inline fun <reified T> decodeRawJson(rawJson: String) = json.decodeFromString<T>(rawJson)

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

    val changeFilesValueIterator = ChangeFilesValueIterator(loadedChangesFiles)
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
          val rawComments = wrapIgnoringErrors("Raw comments changeId=${id}") {
            client.getCommentsRaw(
              baseUrl,
              changeId = id
            )
          }
          rawComments?.let { jsonObjectBuffer.addEntry(it, id) }
          logger.info("Loaded comments for $id")
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

  private fun saveState() = stateOfLoadFile.writeText(json.encodeToString(stateOfLoad))
}
