import client.ClientGerritREST
import entity.rest.gerrit.ChangeGerrit
import entity.rest.gerrit.ChangeMetaData
import entity.rest.gerrit.CommentsREST
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test

class TestRequest {

  private val json = Json { ignoreUnknownKeys = true }
  private val baseUrl = "http://review.openstack.org"


  private fun findChangeWithComments(): ChangeMetaData {
    var offset = 0
    val client = ClientGerritREST()
    var change: ChangeMetaData? = null

    while (change == null) {
      val rawBatch = runBlocking {
        client.getChangesRawLight(
          baseUrl,
          offset
        )
      }
      val batch = json.decodeFromString<List<ChangeMetaData>>(rawBatch)
      change = batch.find { it.totalCommentCount > 0 }
      offset += batch.size
    }
    return change

  }

  @Test
  fun testServerJsonFormat() {
    val client = ClientGerritREST()
    val changeWithCommentsMetaData = findChangeWithComments()

    val rawChangeJson = runBlocking {
      client.getChangeRaw(baseUrl, changeWithCommentsMetaData.number)
    }
    json.decodeFromString<ChangeGerrit>(rawChangeJson)

    val rawCommentsJson = runBlocking {
      client.getCommentsRaw(
        baseUrl,
        changeId = changeWithCommentsMetaData.number
      )
    }
    json.decodeFromString<CommentsREST>(rawCommentsJson)
  }

}