package com.example.shine.data.remote

import com.example.shine.domain.model.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenRefreshSchedulerTest {

    private val sessions = MutableStateFlow<Session?>(null)
    private val refreshedFrom = mutableListOf<String>()

    /** Simulates the server: issues "<token>+" valid for [nextLifetimeSeconds]. */
    private val nextLifetimeSeconds = 360L

    private fun TestScope.start() = backgroundScope.launchProactiveRefresh(
        sessions = sessions,
        nowMillis = { testScheduler.currentTime },
        refresh = { token ->
            refreshedFrom += token
            val newToken = "$token+"
            sessions.value = session(newToken, expireAtSeconds = testScheduler.currentTime / 1_000 + nextLifetimeSeconds)
        },
    )

    @Test
    fun refreshesWhenExpireAtIsReached_thenReschedules() = runTest {
        sessions.value = session("a", expireAtSeconds = 400)
        start()

        advanceTimeBy(99_999); runCurrent()
        assertEquals(emptyList<String>(), refreshedFrom)

        advanceTimeBy(1); runCurrent()
        assertEquals(listOf("a"), refreshedFrom)

        // "a+" lives 360s, so it is refreshed 60s later (5 min before it expires).
        advanceTimeBy(59_999); runCurrent()
        assertEquals(listOf("a"), refreshedFrom)

        advanceTimeBy(1); runCurrent()
        assertEquals(listOf("a", "a+"), refreshedFrom)
    }

    @Test
    fun withinFiveMinutesOfExpiry_refreshesImmediately() = runTest {
        sessions.value = session("a", expireAtSeconds = 200)

        start(); runCurrent()

        assertEquals(listOf("a"), refreshedFrom)
    }

    @Test
    fun alreadyExpiredOnLaunch_refreshesImmediately() = runTest {
        testScheduler.advanceTimeBy(500_000)
        sessions.value = session("a", expireAtSeconds = 100)

        start(); runCurrent()

        assertEquals(listOf("a"), refreshedFrom)
    }

    @Test
    fun newSessionCancelsPendingTimer() = runTest {
        sessions.value = session("a", expireAtSeconds = 400)
        start(); runCurrent()

        sessions.value = session("b", expireAtSeconds = 600)
        advanceTimeBy(200_000); runCurrent()
        assertEquals(emptyList<String>(), refreshedFrom)

        advanceTimeBy(100_000); runCurrent()
        assertEquals(listOf("b"), refreshedFrom)
    }

    @Test
    fun signedOutOrNoExpiry_doesNothing() = runTest {
        sessions.value = session("a", expireAtSeconds = null)
        start()
        advanceTimeBy(1_000_000); runCurrent()

        sessions.value = null
        advanceTimeBy(1_000_000); runCurrent()

        assertEquals(emptyList<String>(), refreshedFrom)
    }

    private fun session(token: String, expireAtSeconds: Long?) =
        Session(token = token, refreshToken = "r", expireAt = expireAtSeconds, user = null)
}
