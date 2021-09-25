package io.enerdstone.vertxmicroservice.shared.extension

import io.enerdstone.vertxmicroservice.shared.constant.CONF
import io.enerdstone.vertxmicroservice.shared.constant.DEFAULT_CONF_FILE
import io.enerdstone.vertxmicroservice.shared.constant.FILE
import io.enerdstone.vertxmicroservice.shared.constant.HOCON
import io.enerdstone.vertxmicroservice.shared.constant.PATH
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.configStoreOptionsOf
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitEvent
import net.pearx.kasechange.toKebabCase
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.concurrent.TimeUnit

const val DEPLOYMENT_DELAY = 5L

suspend fun Verticle.deployAsWorker(
    vertx: Vertx,
    deploymentOptions: DeploymentOptions =
        deploymentOptionsOf(
            workerPoolSize = 5,
            worker = true,
            workerPoolName = this::class.simpleName?.toKebabCase()?.toLowerCase() ?: "app-default-worker"
        )
) {
    try {
        //About 5 seconds delay before deploying verticle
        awaitEvent<Long> { vertx.setTimer(TimeUnit.SECONDS.toMillis(DEPLOYMENT_DELAY), it) }
        vertx.deployVerticle(this, deploymentOptions).await()
    } catch (exception: Exception) {
        LogManager.getLogger(this::class.java).error("APP: error deploying ${this::class.java}", exception)
    }
}

suspend fun Verticle.readConfigurations(config: JsonObject) {
    try {
        val filePath = System.getProperty(CONF) ?: DEFAULT_CONF_FILE
        val options = ConfigRetrieverOptions()
            .addStore(configStoreOptionsOf(type = FILE, format = HOCON, config = JsonObject().put(PATH, filePath)))
        val jsonConfigs = ConfigRetriever.create(vertx, options).config.await()
        config.mergeIn(jsonConfigs)
    } catch (ioException: IOException) {
        LogManager.getLogger(this::class.java).error("APP: error reading configs ${this::class.java}", ioException)
    }
}
