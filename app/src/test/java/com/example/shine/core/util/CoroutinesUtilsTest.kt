package com.example.shine.core.util

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CoroutinesUtilsTest {

    @Test
    fun suspendRunCatching_success_returnsSuccess() {
        val result = suspendRunCatching { "hello" }
        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun suspendRunCatching_error_returnsFailure() {
        val ex = IllegalStateException("error")
        val result = suspendRunCatching { throw ex }
        assertTrue(result.isFailure)
        assertEquals(ex, result.exceptionOrNull())
    }

    @Test
    fun suspendRunCatching_cancellationException_rethrows() {
        try {
            suspendRunCatching { throw CancellationException("cancelled") }
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }
}

