package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeMetaData(
  @SerialName("project") override val project: String,
  @SerialName("_number") override val number: Int,
  @SerialName("updated") override val updated: String,
  @SerialName("total_comment_count") override val totalCommentCount: Int,
  @SerialName("_more_changes") override val moreChanges: Boolean? = null
) : Change {
  override val keyChange = "$project-$number"
}

interface Change {
  val project: String
  val number: Int
  val updated: String
  val totalCommentCount: Int
  val moreChanges: Boolean?
  val keyChange: String
}
