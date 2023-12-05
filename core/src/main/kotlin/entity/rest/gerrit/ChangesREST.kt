package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ChangesREST = List<ChangeGerrit>


@Serializable
data class ChangeGerrit(
  @SerialName("id") val id: String,
  @SerialName("project") override val project: String,
  @SerialName("branch") val branch: String,
  @SerialName("status") val status: String,
  @SerialName("created") val created: String,
  @SerialName("updated") override val updated: String,
  @SerialName("total_comment_count") override val totalCommentCount: Int,
  @SerialName("_number") override val number: Int,
  @SerialName("owner") val owner: UserAccountGerrit,
  @SerialName("reviewers") val reviewers: Reviewers,
  @SerialName("revisions") val revisions: Map<String, Revision>,
  val subject: String,
  @SerialName("_more_changes") override val moreChanges: Boolean? = null
) : Change {
  override val keyChange = "$project-$number"
}

@Serializable
data class FileGerrit(
  @SerialName("lines_inserted") val linesInserted: Int? = null,
  @SerialName("lines_deleted") val linesDeleted: Int? = null,
  val size: Int? = null,
  @SerialName("size_delta") val sizeDelta: Int? = null,
  val status: String? = null
)

@Serializable
data class Reviewers(
  @SerialName("REVIEWER") val reviewers: List<UserAccountGerrit> = emptyList(),
)

@Serializable
data class Revision(
  @SerialName("kind") val kind: String,
  @SerialName("_number") val Number: Int,
  @SerialName("created") val created: String,
  @SerialName("uploader") val uploader: UserAccountGerrit,
  @SerialName("ref") val ref: String,
  @SerialName("files") val files: Map<String, FileGerrit>,
  @SerialName("commit_with_footers") val footer: String? = null,
  val commit: Commit
)

@Serializable
data class Commit(
  val author: User,
  val committer: User
)

