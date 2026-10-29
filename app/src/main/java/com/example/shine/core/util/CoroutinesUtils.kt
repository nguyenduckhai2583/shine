package com.example.shine.core.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes [block] and returns [Result.success] on success.
 * Catches all [Throwable]s and returns [Result.failure], while explicitly
 * re-throwing [CancellationException] to preserve structured concurrency in Kotlin Coroutines.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (e: Throwable) {
    Result.failure(e)
}

