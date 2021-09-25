package io.enerdstone.vertxmicroservice.shared.domain.model.config

import kotlinx.serialization.Serializable

@Serializable
internal data class CircuitBreakerConfig(
  val requestTimeout: Long = -1,
  val resetTimeout: Long = 10,
  val maxFailures: Int = 10,
  val maxRetries: Int = 10,
  val backOffTime: Long = 2500
)
