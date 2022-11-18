package client.exceptions

import com.expediagroup.graphql.client.types.GraphQLClientError

class GraphQLException : Exception {
  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
  constructor(errors: List<GraphQLClientError>) : super(errors.joinToString("\n"))
}