package entity.rest.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserREST(
  @SerialName("login") val login: String,
  @SerialName("type") val type: String
)
