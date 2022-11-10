package entity.rest.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias CommitsREST = List<CommitREST>

@Serializable
data class CommitREST(
  @SerialName("sha") val sha: String,
  @SerialName("parents") val parents: List<Parent>,
)

@Serializable
data class Parent(
  @SerialName("sha") val sha: String,
)

