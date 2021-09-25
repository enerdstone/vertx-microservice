package io.enerdstone.vertxmicroservice.sample

import io.enerdstone.vertxmicroservice.shared.extension.launchMainVerticle
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
internal fun main() {
  launchMainVerticle(MainVerticle::class.java)
}
