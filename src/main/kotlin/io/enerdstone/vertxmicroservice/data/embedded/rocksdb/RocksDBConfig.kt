package io.enerdstone.vertxmicroservice.data.embedded.rocksdb

import kotlinx.serialization.Serializable

@Serializable
data class RocksDBConfig(
  val baseDir: String = "/tmp/rocksdb",
  val file: String = "rocksdb-store.db"
)
