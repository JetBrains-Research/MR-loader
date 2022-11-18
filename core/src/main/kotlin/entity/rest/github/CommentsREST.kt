package entity.rest.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias CommentsREST = List<CommentREST>

@Serializable
data class CommentREST(
  @SerialName("user") val user: UserREST
)
