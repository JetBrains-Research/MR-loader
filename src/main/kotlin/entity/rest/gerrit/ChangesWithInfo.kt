package entity.rest.gerrit

import kotlinx.serialization.Serializable

@Serializable
data class ChangesWithInfo(val before: String, val after: String, val changes: ChangesREST)
