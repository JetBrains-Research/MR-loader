import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.FileCommentREST
import entity.rest.gerrit.Reviewers
import entity.rest.gerrit.UserAccountGerrit
import extractor.ExtractorUtil
import extractor.gerrit.JsonObjectsMapBuffer
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import loader.gerrit.iterators.ChangeFilesValueIterator
import loader.gerrit.iterators.CommentsFilesIterator
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestJsonObjectBuffer {
  private val tmpFolder = File("src/test/tmp/")
  private val numberOfCoroutines = 100
  private val numberOfActions = 50

  @Test
  fun testJsonObjectBufferChanges() {
    tmpFolder.mkdirs()
    val map = massGeneration { generateChange(it) }
    assertEquals(numberOfActions * numberOfCoroutines, map.size)

    val files = ExtractorUtil.getFilesIgnoreHidden(tmpFolder)
    val iterator = ChangeFilesValueIterator(files)
    while (iterator.hasNext()) {
      val savedChange = iterator.next()
      val generatedChange = map.remove(savedChange.number)
      assertEquals(generatedChange, savedChange)
    }
    assertTrue(map.isEmpty())
    cleanup()
  }

  @Test
  fun testJsonObjectBufferComments() {
    tmpFolder.mkdirs()
    val map = massGeneration { generateComments(it) }
    assertEquals(numberOfActions * numberOfCoroutines, map.size)

    val files = ExtractorUtil.getFilesIgnoreHidden(tmpFolder)
    val iterator = CommentsFilesIterator(files)
    while (iterator.hasNext()) {
      val savedChange = iterator.next()
      for ((number, comments) in savedChange) {
        val generatedComments = map.remove(number)
        assertEquals(generatedComments, comments)
      }
    }
    assertTrue(map.isEmpty())
    cleanup()
  }

  private inline fun <reified T> massGeneration(
    crossinline generate: (Int) -> T
  ): ConcurrentHashMap<Int, T> {
    val counter = AtomicInteger()
    val jsonObjectBuffer = JsonObjectsMapBuffer(tmpFolder)
    tmpFolder.mkdirs()
    val map = ConcurrentHashMap<Int, T>()

    runBlocking {
      withContext(Dispatchers.Default) {
        massiveRun(numberOfCoroutines, numberOfActions) {
          val number = counter.incrementAndGet()
          val generatedChange = generate(number)
          val rawJson = Json.encodeToString(generatedChange)
          jsonObjectBuffer.addEntry(rawJson, number)
          map[number] = generatedChange
        }
      }
    }
    jsonObjectBuffer.close()

    return map
  }

  private fun randomField(base: String) = "${base}_${Random.nextInt(0, 5)}"

  private fun generateChange(number: Int) =
    ChangeGerrit(
      randomField("id"),
      randomField("project"),
      randomField("branch"),
      randomField("status"),
      randomField("created"),
      randomField("updated"),
      Random.nextInt(0, 10),
      number,
      UserAccountGerrit(Random.nextInt(0, 10)),
      Reviewers((0..Random.nextInt(0, 3)).map { UserAccountGerrit(it) }),
      mapOf(),
      randomField("subject"),
      null
    )

  private suspend fun massiveRun(numberOfCoroutines: Int, numberOfActions: Int, action: suspend () -> Unit) {
    coroutineScope {
      repeat(numberOfCoroutines) {
        launch {
          repeat(numberOfActions) { action() }
        }
      }
    }
  }

  private fun cleanup() = tmpFolder.deleteRecursively()

  private fun generateFileCommentREST() = FileCommentREST(
    UserAccountGerrit(Random.nextInt(0, 10)),
    randomField("updated"),
    randomField("message"),
    randomField("commitId")
  )

  private fun generateComments(number: Int): Map<String, List<FileCommentREST>> {
    val comments = HashMap<String, List<FileCommentREST>>()
    repeat(Random.nextInt(1, 5)) { keyId ->
      comments["key_${keyId}"] = (0..Random.nextInt(1, 3)).map { generateFileCommentREST() }
    }
    return comments
  }

}