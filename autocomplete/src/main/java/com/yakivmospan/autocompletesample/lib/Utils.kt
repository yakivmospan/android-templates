package com.yakivmospan.autocompletesample.lib

import kotlin.coroutines.cancellation.CancellationException

/**
 * Rethrows [CancellationException] so coroutine cancellation is never swallowed,
 * while keeping the [Result] intact for all other failures.
 */
internal fun <T> Result<T>.reThrowCancellation(): Result<T> =
    onFailure { if (it is CancellationException) throw it }

