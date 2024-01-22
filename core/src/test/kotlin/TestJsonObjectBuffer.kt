import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.Reviewers
import entity.rest.gerrit.UserAccountGerrit
import extractor.ExtractorUtil
import extractor.gerrit.JsonObjectsMapBuffer
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import loader.gerrit.iterators.ChangeFilesValueIterator
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestJsonObjectBuffer {

  private fun randomField(base: String) = "${base}_${Random.nextInt(0, 5)}"
  private val tmpFolder = File("src/test/tmp/")

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

  @Test
  fun testJsonObjectBuffer() {
    val counter = AtomicInteger()
    val jsonObjectBuffer = JsonObjectsMapBuffer(tmpFolder)
    tmpFolder.mkdirs()
    val map = ConcurrentHashMap<Int, ChangeGerrit>()
    val numberOfCoroutines = 100
    val numberOfActions = 50

    runBlocking {
      withContext(Dispatchers.Default) {
        massiveRun(numberOfCoroutines, numberOfActions) {
          val number = counter.incrementAndGet()
          val generatedChange = generateChange(number)
          val rawJson = Json.encodeToString(generatedChange)
          jsonObjectBuffer.addEntry(rawJson, number)
          map[number] = generatedChange
        }
      }
    }
    jsonObjectBuffer.close()

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

}