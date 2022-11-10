package entity.rest.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RateLimitREST(
  @SerialName("resources") val resources: Resources,
  @SerialName("rate") val rate: Rate,
)

@Serializable
data class Resources(
  @SerialName("core") val core: Core,
  @SerialName("search") val search: Search,
  @SerialName("graphql") val graphql: Graphql,
  @SerialName("integration_manifest") val integrationManifest: IntegrationManifest,
  @SerialName("source_import") val sourceImport: SourceImport,
  @SerialName("code_scanning_upload") val codeScanningUpload: CodeScanningUpload,
  @SerialName("actions_runner_registration") val actionsRunnerRegistration: ActionsRunnerRegistration,
  @SerialName("scim") val scim: Scim,
)

@Serializable
data class Core(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class Search(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class Graphql(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class IntegrationManifest(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class SourceImport(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class CodeScanningUpload(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class ActionsRunnerRegistration(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class Scim(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

@Serializable
data class Rate(
  @SerialName("limit") val limit: Int,
  @SerialName("used") val used: Int,
  @SerialName("remaining") val remaining: Int,
  @SerialName("reset") val reset: Int,
)

