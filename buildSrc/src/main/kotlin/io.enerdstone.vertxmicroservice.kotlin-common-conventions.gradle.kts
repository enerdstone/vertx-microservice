import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.0")
    }

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("net.pearx.kasechange:kasechange-jvm:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")

}

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

sourceSets.main { java.srcDirs("src/main/java", "src/main/kotlin") }

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
