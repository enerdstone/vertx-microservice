package io.enerdstone.vertxmicroservice.sample

import io.enerdstone.vertxmicroservice.auth.keycloak.KeycloakAuthVerticle
import io.enerdstone.vertxmicroservice.data.embedded.rocksdb.RocksDBVerticle
import io.enerdstone.vertxmicroservice.shared.MicroServiceVerticle
import io.enerdstone.vertxmicroservice.shared.constant.APPLICATION_SHUTDOWN
import io.enerdstone.vertxmicroservice.shared.constant.AUTH_TOKEN_RECEIVED
import io.enerdstone.vertxmicroservice.shared.constant.ROCKSDB_FIND
import io.enerdstone.vertxmicroservice.shared.constant.ROCKSDB_SAVE
import io.enerdstone.vertxmicroservice.shared.extension.deployAsWorker
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
internal class MainVerticle : MicroServiceVerticle() {
    override suspend fun start() {
        super.start()
        KeycloakAuthVerticle().deployAsWorker(vertx)
        consumeMessage<Boolean>(AUTH_TOKEN_RECEIVED) {
            if (it.body()) {
                launch(vertx.dispatcher()) {
                    RocksDBVerticle().deployAsWorker(vertx)
                    val key = "best-language"
                    sendLocalMessage(ROCKSDB_SAVE, "$key:Kotlin")
                    requestLocalMessage<String>(ROCKSDB_FIND, key) { messageResult ->
                        if (messageResult.failed()) {
                            vertx.exceptionHandler().handle(messageResult.cause())
                            logger.info("APP: Sending message to shut down try again later")
                            sendLocalMessage(APPLICATION_SHUTDOWN, true)
                        } else {
                            logger.info("ROCKSDB: Your best language is: ${messageResult.result().body()}")
                        }
                    }
                }
            }
        }
    }
}
