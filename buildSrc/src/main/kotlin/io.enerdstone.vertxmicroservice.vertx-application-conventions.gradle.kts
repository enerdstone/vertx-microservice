import io.vertx.gradle.vertx

plugins {
    id("io.enerdstone.vertxmicroservice.vertx-common-conventions")
    id("io.vertx.vertx-plugin")
    application
}

vertx {
    vertxVersion = "4.1.4"
    redeploy = true
    jvmArgs = listOf(
        "-Dconf=$rootDir/conf/application-dev.conf",
        "-Djava.net.preferIPv4Stack=true",
        "-Dvertx.infinispan.config=$rootDir/conf/infinispan.xml",
        "-Dvertx.logger-delegate-factory-class-name=Log4j2LogDelegateFactory"
    )
    args = listOf("-cluster")
    debugPort = 6051
}
