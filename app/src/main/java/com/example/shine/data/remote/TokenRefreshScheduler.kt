package com.example.shine.data.remote

import com.example.shine.core.di.ApplicationScope
import com.example.shine.core.di.IoDispatcher
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.domain.model.Session
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds

/**
 * Refreshes the token [REFRESH_BEFORE_EXPIRY] before `expireAt` (epoch seconds),
 * and reschedules whenever the session changes.
 * The reactive 440 path in [AuthInterceptor] still covers anything this misses.
 */
@Singleton
class TokenRefreshScheduler @Inject constructor(
    private val local: SessionLocalDataSource,
    private val tokenRefresher: TokenRefresher,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launchProactiveRefresh(
            sessions = local.session,
            nowMillis = System::currentTimeMillis,
            refresh = { token -> withContext(ioDispatcher) { tokenRefresher.refresh(token) } },
        )
    }
}

internal val REFRESH_BEFORE_EXPIRY = 5.minutes

internal fun CoroutineScope.launchProactiveRefresh(
    sessions: Flow<Session?>,
    nowMillis: () -> Long,
    refresh: suspend (expiredToken: String) -> Unit,
): Job = launch {
    sessions
        .map { session -> session?.let { it.token to it.expireAt } }
        .distinctUntilChanged()
        .collectLatest { tokenAndExpiry ->
            val (token, expireAt) = tokenAndExpiry ?: return@collectLatest
            if (expireAt == null) return@collectLatest

            val refreshAtMillis = expireAt * 1_000 - REFRESH_BEFORE_EXPIRY.inWholeMilliseconds
            delay((refreshAtMillis - nowMillis()).coerceAtLeast(0).milliseconds)
            refresh(token)
        }
}
