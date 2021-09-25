package io.enerdstone.vertxmicroservice.data.embedded.rocksdb

import io.enerdstone.vertxmicroservice.data.domain.repository.KeyValueRepository
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.apache.logging.log4j.LogManager
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksDBException
import java.io.File
import java.io.IOException
import java.nio.file.Files

@ExperimentalSerializationApi
class RocksDBRepository(rocksDBConfig: RocksDBConfig) : KeyValueRepository<String, String> {

  private val logger = LogManager.getLogger(this::class.java)

  private lateinit var rocksDb: RocksDB

  init {
    RocksDB.loadLibrary()
    try {
      val baseDir = File(rocksDBConfig.baseDir, rocksDBConfig.file)
      if (!baseDir.exists()) {
        Files.createDirectories(baseDir.parentFile.toPath())
        Files.createDirectories(baseDir.absoluteFile.toPath())
      }
      rocksDb = RocksDB.open(Options().apply { setCreateIfMissing(true) }, baseDir.absolutePath)
      logger.info("ROCKSDB: RocksDB initialized")
    } catch (ioException: IOException) {
      logger.error("ROCKSDB: Error creating RocksDB database file", ioException)
    } catch (rocksDBException: RocksDBException) {
      logger.error("ROCKSDB: Error initializing RocksDB database", rocksDBException)
    }
  }

  override fun save(key: String, value: String) {
    try {
      rocksDb.put(key.toByteArray(), Cbor {}.encodeToByteArray(value))
    } catch (rocksDBException: RocksDBException) {
      logger.error("ROCKSDB: Cannot value for key: $key", rocksDBException)
    }
  }

  override fun find(key: String): String? {
    return try {
      val bytes = rocksDb.get(key.toByteArray())
      if (bytes != null) Cbor.decodeFromByteArray(bytes) else null
    } catch (rocksDBException: RocksDBException) {
      logger.error("ROCKSDB: Value not found for key: $key", rocksDBException)
      return null
    }
  }

  override fun delete(key: String): Boolean {
    return try {
      rocksDb.delete(key.toByteArray())
      true
    } catch (rocksDBException: RocksDBException) {
      logger.error("ROCKSDB: Error deleting key", rocksDBException)
      false
    }
  }

  override fun destroy() {
    rocksDb.close()
  }
}
