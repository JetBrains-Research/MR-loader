package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ReviewersREST = List<Review>


@Serializable
data class Review(
  @SerialName("approvals") val approvals: Approvals? = null,
  @SerialName("_account_id") val accountId: Int,
  @SerialName("name") val name: String = "",
  @SerialName("email") val email: String = "",
  @SerialName("username") val username: String = "",
)

@Serializable
data class Approvals(
  @SerialName("Verified") val verified: String = "",
  @SerialName("Code-Review") val codeReview: String = "",
)

