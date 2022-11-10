package entity.rest.gerrit

import kotlinx.serialization.Serializable

@Serializable
data class ChangeData(
  val changeGerrit: ChangeGerrit,
  val commentsREST: CommentsREST? = null
)
