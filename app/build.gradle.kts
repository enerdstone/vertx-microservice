import io.vertx.gradle.vertx

plugins{
  id("io.enerdstone.vertxmicroservice.vertx-application-conventions")
}

dependencies {
  implementation(project(":core"))
}

vertx {
  launcher = "io.enerdstone.vertxmicroservice.discovery.AppLauncher"
  mainVerticle = "io.enerdstone.vertxmicroservice.app.MainVerticle"
  debugPort = 6000
}
