package io.enerdstone.vertxmicroservice.shared.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ServiceResponse(
    val succeeded: Boolean = true,
    var statusCode: Int = 500,
    @Contextual
    var payload: Any?
)
