package io.enerdstone.vertxmicroservice.shared.extension

import io.enerdstone.vertxmicroservice.AppLauncher
import io.vertx.core.Verticle

fun <T : Verticle> launchMainVerticle(clazz: Class<T>, configsFile: String = "conf/service-dev.conf") {
    val projectDir = System.getProperty("user.dir")
    val args = listOf(
        "run", clazz.name,
        "--redeploy=src/**/*",
        "-cluster",
        "--launcher-class=${AppLauncher::class.java.name}",
        "-Dconf=$configsFile",
        "-Djava.net.preferIPv4Stack=true",
        "-Dvertx.infinispan.config=conf/infinispan.xml",
        "-Dvertx.logger-delegate-factory-class-name=Log4j2LogDelegateFactory",
    ).apply { if (!projectDir.isNullOrBlank()) this.plus("--on-redeploy=$projectDir/gradlew classes") }

    AppLauncher.dispatch(args.toTypedArray())
}
