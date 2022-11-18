package entity.rest.gerrit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAccountGerrit(
  @SerialName("_account_id") val accountId: Int,
) : User()

@Serializable
open class User {
  val name: String = ""
  val email: String = ""
  val username: String = ""
  val keyUser = "${name}:${email}:${username}"
}
