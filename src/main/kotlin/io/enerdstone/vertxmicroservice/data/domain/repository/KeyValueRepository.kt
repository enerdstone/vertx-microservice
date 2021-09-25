package io.enerdstone.vertxmicroservice.data.domain.repository

/**
 * A key value repository that exposes methods for database CRUD Operations. [K] is the key type,
 * [V] represents the value type
 */
interface KeyValueRepository<K, V> {

  /** Save [value] for [key] in the rocksdb database */
  fun save(key: K, value: V)

  /** Find value of type [V] for the given key or return null */
  fun find(key: K): V?

  /** Delete value for the provided [key] return true false otherwise */
  fun delete(key: K): Boolean

  /**
   * Close the database
   */
  fun destroy()
}
