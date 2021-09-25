package io.enerdstone.vertxmicroservice.shared.domain.provider

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Encapsulate [Dispatchers] provider.
 *
 * [default] Use to run CPU intensive tasks in the background
 *
 * [io] Use to run input/output related tasks in the background
 */
interface DispatcherProvider {
  fun default(): CoroutineDispatcher = Dispatchers.Default
  fun io(): CoroutineDispatcher = Dispatchers.IO
}
