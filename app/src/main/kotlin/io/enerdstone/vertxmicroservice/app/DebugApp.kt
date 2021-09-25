package io.enerdstone.vertxmicroservice.app

import io.enerdstone.vertxmicroservice.shared.extension.launchMainVerticle
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
internal fun main() {
  launchMainVerticle(MainVerticle::class.java)
}
