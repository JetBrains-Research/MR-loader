package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


typealias CommentsREST = Map<String, List<FileCommentREST>>

@Serializable
data class FileCommentREST(
  @SerialName("author") val author: UserAccountGerrit,
//@SerialName("change_message_id")
//@SerialName("unresolved"): true,
//@SerialName("patch_set"): 1,
//@SerialName("id"): "9efe6524_789aef27",
  @SerialName("updated") val updated: String,
  @SerialName("message") val message: String,
  @SerialName("commit_id") val commitId: String
)
