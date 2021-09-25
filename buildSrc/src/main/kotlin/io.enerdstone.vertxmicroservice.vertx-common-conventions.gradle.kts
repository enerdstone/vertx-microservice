plugins {
    id("io.enerdstone.vertxmicroservice.kotlin-common-conventions")
}

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("io.vertx:vertx-stack-depchain:4.1.4")
    }

    // Align versions of vertx dependencies
    implementation(platform("io.vertx:vertx-stack-depchain"))
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
    implementation("org.rocksdb:rocksdbjni:6.22.1.1")

    // Use Vertx Jupiter API for testing.
    testImplementation("io.vertx:vertx-junit5")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
}
