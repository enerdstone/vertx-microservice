package io.enerdstone.vertxmicroservice.shared.domain.model.config

import kotlinx.serialization.Serializable

@Serializable
data class HttpEndpointConfig(val name: String, val root: String)
