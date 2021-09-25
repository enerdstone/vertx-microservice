package io.enerdstone.vertxmicroservice.data.embedded.rocksdb

import io.enerdstone.vertxmicroservice.shared.MicroServiceVerticle
import io.enerdstone.vertxmicroservice.shared.constant.ROCKSDB
import io.enerdstone.vertxmicroservice.shared.constant.ROCKSDB_FIND
import io.enerdstone.vertxmicroservice.shared.constant.ROCKSDB_SAVE
import io.enerdstone.vertxmicroservice.shared.extension.decodeJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException

@ExperimentalSerializationApi
class RocksDBVerticle : MicroServiceVerticle() {

  private lateinit var rocksDbRepository: RocksDBRepository

  override suspend fun start() {
    super.start()
    try {
      rocksDbRepository = RocksDBRepository(config.getJsonObject(ROCKSDB).toString().decodeJson())
    } catch (serializationException: SerializationException) {
      vertx.exceptionHandler().handle(serializationException)
    }

    consumeMessage<String>(ROCKSDB_SAVE) {
      //save key:value
      val messageSplit = it.body().split(":")
      rocksDbRepository.save(messageSplit.first(), messageSplit.last())
    }
    consumeMessage<String>(ROCKSDB_FIND) { it.reply(rocksDbRepository.find(it.body() ?: "0")) }
  }

  override suspend fun stop() {
    rocksDbRepository.destroy()
  }
}
