import io.vertx.gradle.vertx

plugins{
  id("io.enerdstone.vertxmicroservice.vertx-application-conventions")
}

dependencies {
  implementation(project(":lib"))
}

vertx {
  launcher = "io.enerdstone.vertxmicroservice.discovery.AppLauncher"
  mainVerticle = "io.enerdstone.vertxmicroservice.sample.MainVerticle"
  debugPort = 6000
}
