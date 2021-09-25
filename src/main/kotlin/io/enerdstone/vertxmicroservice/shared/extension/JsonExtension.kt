package io.enerdstone.vertxmicroservice.shared.extension

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val json = Json {
  encodeDefaults = true
  ignoreUnknownKeys = true
}

/** Decode string to an entity of type [T] */
inline fun <reified T> String.decodeJson(): T = json.decodeFromString(this)

/** Encode the type [T] into a Json string */
inline fun <reified T> T.encodeJson(): String = json.encodeToString(this)

/**
 * Converts the [this] [String] to [JsonElement]
 */
fun String.parseToJsonElement(): JsonElement = json.parseToJsonElement(this)
