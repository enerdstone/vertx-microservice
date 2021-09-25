package io.enerdstone.vertxmicroservice.shared

import io.vertx.core.json.JsonObject as VertxJsonObject
import io.enerdstone.vertxmicroservice.shared.constant.ACCEPT
import io.enerdstone.vertxmicroservice.shared.constant.ACCESS_TOKEN
import io.enerdstone.vertxmicroservice.shared.constant.APPLICATION_JSON
import io.enerdstone.vertxmicroservice.shared.constant.APPLICATION_SHUTDOWN
import io.enerdstone.vertxmicroservice.shared.constant.CIRCUIT_BREAKER
import io.enerdstone.vertxmicroservice.shared.constant.CONTENT_TYPE
import io.enerdstone.vertxmicroservice.shared.constant.DEFAULT_CIRCUIT_BREAKER_NAME
import io.enerdstone.vertxmicroservice.shared.constant.SERVICE
import io.enerdstone.vertxmicroservice.shared.constant.SHARED_APPLICATION_STATE
import io.enerdstone.vertxmicroservice.shared.constant.SHARED_EXCEPTION_STATE
import io.enerdstone.vertxmicroservice.shared.domain.model.ServiceResponse
import io.enerdstone.vertxmicroservice.shared.domain.model.config.CircuitBreakerConfig
import io.enerdstone.vertxmicroservice.shared.domain.model.config.HttpEndpointConfig
import io.enerdstone.vertxmicroservice.shared.domain.model.config.MicroServiceConfig
import io.enerdstone.vertxmicroservice.shared.extension.decodeJson
import io.enerdstone.vertxmicroservice.shared.extension.readConfigurations
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.VertxException
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.auth.User
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.multipart.MultipartForm
import io.vertx.kotlin.circuitbreaker.circuitBreakerOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.web.client.webClientOptionsOf
import io.vertx.kotlin.servicediscovery.serviceDiscoveryOptionsOf
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.types.HttpEndpoint
import io.vertx.servicediscovery.types.MessageSource
import net.pearx.kasechange.toKebabCase
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

abstract class MicroServiceVerticle : CoroutineVerticle() {

  protected lateinit var sharedApplicationData: AsyncMap<String, Any>

  open lateinit var sharedExceptionState: AsyncMap<String, VertxJsonObject>

  open lateinit var webClient: WebClient

  open lateinit var circuitBreaker: CircuitBreaker

  open lateinit var microServiceConfig: MicroServiceConfig

  lateinit var serviceDiscovery: ServiceDiscovery

  protected val logger: Logger = LogManager.getLogger(this::class.java)

  override suspend fun start() {
    logger.info(logMessage("${this::class.java.simpleName.toKebabCase()} deployed with deployment id $deploymentID"))
    try {
      sharedApplicationData = vertx.sharedData().getAsyncMap<String, Any>(SHARED_APPLICATION_STATE).await()
      sharedExceptionState = vertx.sharedData().getAsyncMap<String, VertxJsonObject>(SHARED_EXCEPTION_STATE).await()

      vertx.exceptionHandler { handleError(it) }

      this.readConfigurations(config)

      microServiceConfig = config.getJsonObject(SERVICE).toString().decodeJson()

      initCircuitBreaker()

      webClient =
        WebClient.wrap(
          vertx.createHttpClient(),
          webClientOptionsOf(keepAlive = true, verifyHost = microServiceConfig.verifyHost)
        )

      val (announceAddress, serviceDiscoveryName) = microServiceConfig.serviceDiscovery
      serviceDiscovery = ServiceDiscovery.create(
        vertx, serviceDiscoveryOptionsOf(announceAddress = announceAddress, name = serviceDiscoveryName)
      )
    } catch (exception: Exception) {
      vertx.exceptionHandler().handle(exception)
    }
  }

  override suspend fun stop() {
    logger.info(logMessage("Stopping ${this::class.java.simpleName.toKebabCase()} verticle"))
    serviceDiscovery.close()
  }

  private fun initCircuitBreaker() {
    val (requestTimeout, resetTimeout, maxFailures, maxRetries, backOffTime) =
      config.getJsonObject(CIRCUIT_BREAKER).toString().decodeJson<CircuitBreakerConfig>()

    val circuitBreakerOptions =
      circuitBreakerOptionsOf(
        timeout = requestTimeout,
        resetTimeout = resetTimeout,
        maxFailures = maxFailures,
        maxRetries = maxRetries,
        fallbackOnFailure = true
      )

    circuitBreaker =
      CircuitBreaker.create(DEFAULT_CIRCUIT_BREAKER_NAME, vertx, circuitBreakerOptions)
        .retryPolicy { count ->
          val retryTime = count * backOffTime.secondsToMillis()
          val seconds = TimeUnit.SECONDS.convert(retryTime, TimeUnit.MILLISECONDS)
          logger.warn(logMessage("Connection failed. Retrying after $seconds second${if (seconds > 1) "s" else ""}."))
          retryTime
        }
        .fallback {
          logger.error(logMessage("Server not responding. Reached maximum retries $maxRetries after $maxFailures failures."))
          logger.error(logMessage("Server is unreachable. Check if the server is running then restart service."))
          logger.error(logMessage("Shutting down gracefully..."))
          sendLocalMessage(APPLICATION_SHUTDOWN, true)
        }
        .openHandler { logger.info(logMessage("$DEFAULT_CIRCUIT_BREAKER_NAME opened")) }
        .closeHandler { logger.info(logMessage("$DEFAULT_CIRCUIT_BREAKER_NAME closed")) }
  }

  protected fun webRequest(
    method: HttpMethod = HttpMethod.GET,
    url: String,
    payload: Any? = null,
    queryParams: Map<String, String> = mapOf(),
    user: User? = null,
    handler: Handler<AsyncResult<HttpResponse<Buffer>?>>
  ) {
    if (user == null || user.expired()) {
      logger.warn(logMessage("Session user is null or expired."))
      return
    }

    val httpRequest =
      httpRequest(method, url)
        .putHeader(ACCEPT, APPLICATION_JSON)
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .followRedirects(true)
        .bearerTokenAuthentication(user.getAccessToken())
        .timeout(microServiceConfig.httpClientTimeout.secondsToMillis())

    if (queryParams.isNotEmpty())
      queryParams.forEach { httpRequest.addQueryParam(it.key, it.value.encodeUrl()) }

    when (payload) {
      is String -> httpRequest.sendBuffer(Buffer.buffer(payload), handler)
      is MultipartForm -> httpRequest.sendMultipartForm(payload, handler)
      is JsonArray -> httpRequest.sendBuffer(payload.toBuffer(), handler)
      is VertxJsonObject -> httpRequest.sendBuffer(payload.toBuffer(), handler)
      null -> httpRequest.send(handler)
    }
  }

  private fun httpRequest(httpMethod: HttpMethod, url: String): HttpRequest<Buffer> =
    when (httpMethod) {
      HttpMethod.POST -> webClient.postAbs(url)
      HttpMethod.DELETE -> webClient.deleteAbs(url)
      HttpMethod.PUT -> webClient.putAbs(url)
      HttpMethod.GET -> webClient.getAbs(url)
      HttpMethod.PATCH -> webClient.patchAbs(url)
      else -> webClient.headAbs(url)
    }

  private fun User.getAccessToken() = this.principal().getString(ACCESS_TOKEN)

  private fun String.encodeUrl() = URLEncoder.encode(this, Charsets.UTF_8.toString())

  protected fun Long.secondsToMillis() = TimeUnit.SECONDS.toMillis(this)

  protected fun <T> consumeMessage(address: String, handler: (Message<T>) -> Unit) {
    vertx.eventBus().consumer<T>(address) {
      handler(it)
    }
  }

  open fun sendLocalMessage(address: String, content: Any) {
    try {
      vertx.eventBus().send(address, content)
    } catch (vertxException: VertxException) {
      vertx.exceptionHandler().handle(vertxException)
    }
  }

  open fun <T> requestLocalMessage(address: String, content: Any, replyHandler: Handler<AsyncResult<Message<T>>>) {
    try {
      vertx.eventBus().request(address, content, replyHandler)
    } catch (vertxException: ReplyException) {
      vertx.exceptionHandler().handle(vertxException)
    }
  }

  open fun publishLocalMessage(address: String, content: Boolean) {
    try {
      vertx.eventBus().publish(address, content)
    } catch (vertxException: VertxException) {
      vertx.exceptionHandler().handle(vertxException)
    }
  }

  /**
   * Register [httpEndpointConfigs] to make them discoverable via [ServiceDiscovery]
   */
  fun registerHttpEndpoints(
    host: String,
    port: Int,
    baseUrlPath: String,
    httpEndpointConfigs: List<HttpEndpointConfig>
  ) {
    httpEndpointConfigs.forEach { httpEndpointConfig: HttpEndpointConfig ->
      val record =
        HttpEndpoint.createRecord(httpEndpointConfig.name, host, port, baseUrlPath + httpEndpointConfig.root)
      serviceDiscovery.publish(record).onSuccess {
        logger.info(logMessage("http endpoint at path ${httpEndpointConfig.root} successfully registered"))
      }.onFailure {
        logger.warn(logMessage("failed to create http endpoint for: ${httpEndpointConfig.root}"))
        handleError(it)
      }
    }
  }

  /**
   * Register [serviceName] to the provided [eventBusAddress] to make it accessible
   */
  fun registerMessageConsumer(serviceName: String, eventBusAddress: String) {
    val schemaServiceRecord = MessageSource.createRecord(serviceName, eventBusAddress)
    serviceDiscovery.publish(schemaServiceRecord)
      .onSuccess {
        logger.info(logMessage("successfully registered service '$serviceName' to address '$eventBusAddress'"))
      }
      .onFailure {
        logger.warn(logMessage("failed to register service '$serviceName' to address '$eventBusAddress'"))
        handleError(it)
      }
  }

  open fun handleError(throwable: Throwable): ServiceResponse {
    logger.error(throwable.stackTraceToString())
    return ServiceResponse(succeeded = false, payload = throwable.localizedMessage)
  }

  open fun logMessage(message: String) = "APP: $message"
}
