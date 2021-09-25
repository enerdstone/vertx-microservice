plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.5.0")
    implementation("gradle.plugin.io.vertx:vertx-gradle-plugin:1.3.0")
}
