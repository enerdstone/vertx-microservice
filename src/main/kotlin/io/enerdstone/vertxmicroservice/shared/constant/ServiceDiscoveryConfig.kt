package io.enerdstone.vertxmicroservice.shared.constant

import kotlinx.serialization.Serializable

@Serializable
data class ServiceDiscoveryConfig(
    val announceAddress: String = "app.discovery.address",
    val name: String = "app-service-discovery"
)
