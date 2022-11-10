package entity.rest.github

data class PRInfo(
  val author: UserProfile,
  val number: Int,
  val createdAt: String,
  val closedAt: String,
  val reviewers: Set<UserProfile>,
  val commits: List<String>
)
