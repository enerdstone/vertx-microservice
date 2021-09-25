import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.5.21"
  kotlin("plugin.serialization") version "1.5.21"
  id("io.vertx.vertx-plugin") version "1.3.0"
  application
}

group = "io.enerdstone"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.1.4"
val junitJupiterVersion = "5.7.0"

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-service-discovery")
  implementation("io.vertx:vertx-infinispan")
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-config-hocon")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-circuit-breaker")
  implementation("io.vertx:vertx-auth-oauth2")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("org.apache.logging.log4j:log4j-core:2.14.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.2")
  implementation("org.rocksdb:rocksdbjni:6.22.1.1")
  implementation("net.pearx.kasechange:kasechange-jvm:1.3.0")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

vertx {
  launcher = "io.enerdstone.vertxmicroservice.AppLauncher"
  mainVerticle = "io.enerdstone.vertxmicroservice.sample.MainVerticle"
  debugPort = 6040
  redeploy = true
  jvmArgs = listOf(
    "-Dconf=$rootDir/conf/service-dev.conf",
    "-Djava.net.preferIPv4Stack=true",
    "-Dvertx.infinispan.config=$rootDir/conf/infinispan.xml",
    "-Dvertx.logger-delegate-factory-class-name=Log4j2LogDelegateFactory"
  )
  args = listOf("-cluster")
}
