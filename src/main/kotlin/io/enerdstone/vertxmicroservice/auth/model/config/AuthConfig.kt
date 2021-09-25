package io.enerdstone.vertxmicroservice.auth.model.config

import kotlinx.serialization.Serializable

@Serializable
internal data class AuthConfig(
  val clientId: String,
  val clientSecret: String,
  val realm: String,
  val baseUrl: String,
  val username: String,
  val password: String
) {
  fun site() = "$baseUrl/auth/realms/$realm"
}
