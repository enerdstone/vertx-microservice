package io.enerdstone.vertxmicroservice

import io.vertx.core.Launcher
import io.vertx.core.VertxOptions
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager
import org.apache.logging.log4j.LogManager
import org.infinispan.manager.DefaultCacheManager

object AppLauncher : Launcher() {

    private val logger = LogManager.getLogger(AppLauncher::class)

    override fun beforeStartingVertx(options: VertxOptions) {
        logger.info("[LAUNCHER] starting vertx application")
        options.apply {
            clusterManager = InfinispanClusterManager(DefaultCacheManager())
        }
    }
}
