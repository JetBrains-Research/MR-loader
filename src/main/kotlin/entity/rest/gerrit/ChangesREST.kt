package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ChangesREST = List<ChangeGerrit>

@Serializable
data class ChangeGerrit(
  @SerialName("id") val id: String,
  @SerialName("project") val project: String,
  @SerialName("branch") val branch: String,
//  @SerialName("topic") val topic: String = "",
//  @SerialName("change_id") val changeId: String,
  @SerialName("status") val status: String,
  @SerialName("created") val created: String,
  @SerialName("updated") val updated: String,
//  @SerialName("submitted") val submitted: String = "",
//  @SerialName("submitter") val submitter: Submitter = Submitter(-1),
  @SerialName("total_comment_count") val totalCommentCount: Int,
//  @SerialName("unresolved_comment_count") val unresolvedCommentCount: Int,
//  @SerialName("has_review_started") val hasReviewStarted: Boolean,
//  @SerialName("submission_id") val submissionId: String = "",
//  @SerialName("meta_rev_id") val metaRevId: String,
  @SerialName("_number") val number: Int,
  @SerialName("owner") val owner: UserAccountGerrit,
//  @SerialName("labels") val labels: Labels,
  @SerialName("reviewers") val reviewers: Reviewers,
  @SerialName("revisions") val revisions: Map<String, Revision>,
  val subject: String,
  @SerialName("_more_changes") val moreChanges: Boolean? = null
) {
  val keyChange = "$project-$number"
}

@Serializable
data class FileGerrit(
  @SerialName("lines_inserted") val linesInserted: Int? = null,
  @SerialName("lines_deleted") val linesDeleted: Int? = null,
  val size: Int? = null,
  @SerialName("size_delta") val sizeDelta: Int? = null,
  val status: String? = null
)
// TODO: think about
//@Serializable data class Labels(
//  @SerialName("Verified") val Verified: Verified,
//  @SerialName("Code-Review") val Code-Review: Code-Review,
//@SerialName("Workflow") val Workflow: Workflow,
//)

//@Serializable data class Verified(
//  @SerialName("all") val all: List<Al>,
//  @SerialName("values") val values: Values,
//  @SerialName("default_value") val defaultValue: Int,
//)
//
//@Serializable data class Al(
//  @SerialName("tag") val tag: String,
//  @SerialName("value") val value: Int,
//  @SerialName("date") val date: String,
//  @SerialName("permitted_voting_range") val permittedVotingRange: PermittedVotingRange,
//  @SerialName("_account_id") val AccountId: Int,
//)


//@Serializable data class Values(
//  @SerialName("-2") val -2: String,
//  @SerialName("-1") val -1: String,
//  @SerialName(" 0") val  0: String,
//  @SerialName("+1") val +1: String,
//  @SerialName("+2") val +2: String,
//)

//@Serializable data class Code-Review(
//@SerialName("all") val all: List<Al>,
//@SerialName("values") val values: Values,
//@SerialName("default_value") val defaultValue: Int,
//)
//
//@Serializable data class Al(
//  @SerialName("value") val value: Int,
//  @SerialName("_account_id") val AccountId: Int,
//)
//
//@Serializable data class Values(
//  @SerialName("-2") val -2: String,
//  @SerialName("-1") val -1: String,
//  @SerialName(" 0") val  0: String,
//  @SerialName("+1") val +1: String,
//  @SerialName("+2") val +2: String,
//)

//@Serializable data class Workflow(
//  @SerialName("all") val all: List<Al>,
//  @SerialName("values") val values: Values,
//  @SerialName("default_value") val defaultValue: Int,
//)

//@Serializable data class Al(
//  @SerialName("value") val value: Int,
//  @SerialName("_account_id") val AccountId: Int,
//)

//@Serializable data class Values(
//  @SerialName("-1") val -1: String,
//  @SerialName(" 0") val  0: String,
//  @SerialName("+1") val +1: String,
//)

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

