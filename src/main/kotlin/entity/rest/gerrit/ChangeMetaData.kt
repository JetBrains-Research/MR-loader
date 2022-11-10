package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeMetaData(
  @SerialName("project") val project: String,
  @SerialName("_number") val number: Int,
  @SerialName("updated") val updated: String,
  @SerialName("total_comment_count") val totalCommentCount: Int,
  @SerialName("_more_changes") val moreChanges: Boolean? = null

) {
  val keyChange = "$project-$number"
}
