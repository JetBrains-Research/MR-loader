package entity.rest.github

import entity.rest.gerrit.UserAccountGerrit
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
  val login: String = "",
  val name: String = "",
  val email: String = ""
) {
  constructor(userAccountGerrit: UserAccountGerrit) : this(
    userAccountGerrit.username,
    userAccountGerrit.name,
    userAccountGerrit.email
  )
}
