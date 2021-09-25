package io.enerdstone.vertxmicroservice.shared.domain.model.config

import io.enerdstone.vertxmicroservice.shared.constant.ServiceDiscoveryConfig
import kotlinx.serialization.Serializable

@Serializable
data class MicroServiceConfig(
    val verifyHost: Boolean = false,
    val httpClientTimeout: Long = -1,
    val serviceDiscovery: ServiceDiscoveryConfig = ServiceDiscoveryConfig()
)
