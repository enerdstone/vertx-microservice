package io.enerdstone.vertxmicroservice.auth.keycloak

import io.enerdstone.vertxmicroservice.auth.model.config.AuthConfig
import io.enerdstone.vertxmicroservice.shared.MicroServiceVerticle
import io.enerdstone.vertxmicroservice.shared.constant.APPLICATION_SHUTDOWN
import io.enerdstone.vertxmicroservice.shared.constant.AUTH_DEPLOY
import io.enerdstone.vertxmicroservice.shared.constant.AUTH_TOKEN_RECEIVED
import io.enerdstone.vertxmicroservice.shared.constant.EXPIRES_IN
import io.enerdstone.vertxmicroservice.shared.constant.KEYCLOAK
import io.enerdstone.vertxmicroservice.shared.constant.PASSWORD
import io.enerdstone.vertxmicroservice.shared.constant.USER
import io.enerdstone.vertxmicroservice.shared.constant.USERNAME
import io.enerdstone.vertxmicroservice.shared.constant.VERTICLE_UNDEPLOY
import io.enerdstone.vertxmicroservice.shared.extension.decodeJson
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.oauth2.OAuth2FlowType
import io.vertx.ext.auth.oauth2.OAuth2Options
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * This subclass of [MicroServiceVerticle] is responsible for receiving and refreshing Keycloak
 * access token. Once a new access token is received, a message is published to the event bus.
 */
class KeycloakAuthVerticle : MicroServiceVerticle() {

  override suspend fun start() {
    super.start()
    launch(vertx.dispatcher()) {
      val keycloakConfig = config.getJsonObject(KEYCLOAK).toString().decodeJson<AuthConfig>()
      oauth2Auth(keycloakConfig)
        ?.authenticate(
          JsonObject().put(USERNAME, keycloakConfig.username).put(PASSWORD, keycloakConfig.password)
        )
        ?.onSuccess { user: User ->
          logger.info(logMessage("User authenticated"))
          updateSessionUser(user = user)
          refreshTokenPeriodically(
            keycloakConfig = keycloakConfig,
            expiryPeriod = user.principal().getLong(EXPIRES_IN)
          )
        }
        ?.onFailure {
          vertx.exceptionHandler().handle(it)
          redeploy()
        }
    }
  }

  private fun redeploy() {
    launch(vertx.dispatcher()) {
      val deployVerticleMsg = awaitResult<Message<Boolean>> { requestLocalMessage(AUTH_DEPLOY, true, it) }
      if (deployVerticleMsg.body()) sendLocalMessage(VERTICLE_UNDEPLOY, deploymentID)
    }
  }

  private suspend fun oauth2Auth(keycloakConfig: AuthConfig) =
    try {
      withContext(vertx.dispatcher()) {
        val oAuth2Options =
          OAuth2Options().apply {
            clientId = keycloakConfig.clientId
            clientSecret = keycloakConfig.clientSecret
            site = keycloakConfig.site()
            tenant = keycloakConfig.realm
            flow = OAuth2FlowType.PASSWORD
            isValidateIssuer = false
          }
        KeycloakAuth.discover(vertx, oAuth2Options)
      }
        .await()
    } catch (ioException: IOException) {
      vertx.exceptionHandler().handle(ioException)
      sendLocalMessage(APPLICATION_SHUTDOWN, true)
      null
    }

  private fun refreshTokenPeriodically(keycloakConfig: AuthConfig, expiryPeriod: Long) {
    launch(vertx.dispatcher()) {
      val user: User? = sharedApplicationData.get(USER).await() as User?
      oauth2Auth(keycloakConfig)?.run {
        vertx.setPeriodic(expiryPeriod.secondsToMillis()) {
          if (user != null && user.expired()) {
            refresh(user)
              .onSuccess {
                logger.info(logMessage("Access token refreshed"))
                updateSessionUser(it)
              }
              .onFailure {
                vertx.exceptionHandler().handle(it)
                redeploy()
              }
          }
        }
      }
    }
  }

  private fun updateSessionUser(user: User) {
    sharedApplicationData
      .put(USER, user)
      .onSuccess { logger.info(logMessage("Session user updated")) }
      .onFailure { vertx.exceptionHandler().handle(it) }
    publishLocalMessage(AUTH_TOKEN_RECEIVED, true)
  }

  override fun logMessage(message: String) = "AUTH: $message"
}
