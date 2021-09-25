package io.enerdstone.vertxmicroservice.shared.domain.model.config

import kotlinx.serialization.Serializable

@Serializable
data class MicroServiceConfig(
    val verifyHost: Boolean = false,
    val httpClientTimeout: Long = -1,
    val serviceDiscovery: ServiceDiscoveryConfig = ServiceDiscoveryConfig()
)
