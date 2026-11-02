package com.example.shine.data.remote.socket

import android.util.Log
import com.example.shine.core.di.ApplicationScope
import com.example.shine.core.di.IoDispatcher
import com.example.shine.core.util.AppLifecycleMonitor
import com.example.shine.core.util.NetworkMonitor
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.domain.model.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketLifecycleManager @Inject constructor(
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val appLifecycleMonitor: AppLifecycleMonitor,
    private val networkMonitor: NetworkMonitor,
    private val socketService: SocketService,
    private val mediators: Set<@JvmSuppressWildcards SocketMediator>,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private var lifecycleJob: Job? = null
    private var eventsJob: Job? = null

    private data class SocketContext(
        val session: Session?,
        val isInForeground: Boolean,
        val isOnline: Boolean,
    )

    fun start() {
        if (lifecycleJob?.isActive == true) return

        startEventDispatching()

        lifecycleJob = scope.launch {
            combine(
                sessionLocalDataSource.session,
                appLifecycleMonitor.isInForeground,
                networkMonitor.isOnline,
            ) { session, isInForeground, isOnline ->
                SocketContext(session, isInForeground, isOnline)
            }
                .distinctUntilChanged()
                .collect { (session, isInForeground, isOnline) ->
                    val token = session?.token
                    val userId = session?.user?.id
                    val workspaceId = session?.workspaceId

                    val canConnect = !token.isNullOrBlank() &&
                        !userId.isNullOrBlank() &&
                        isInForeground &&
                        isOnline

                    if (canConnect) {
                        socketService.reconnect(
                            token = token,
                            userId = userId,
                            workspaceId = workspaceId,
                        )
                    } else {
                        socketService.disconnect()
                    }
                }
        }
    }

    private fun startEventDispatching() {
        if (eventsJob?.isActive == true) return
        eventsJob = scope.launch(ioDispatcher) {
            socketService.events.collect { event ->
                mediators.forEach { mediator ->
                    try {
                        mediator.handleEvent(event)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [SOCKET] Mediator error [${mediator::class.simpleName}]: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun stop() {
        lifecycleJob?.cancel()
        lifecycleJob = null
        eventsJob?.cancel()
        eventsJob = null
        socketService.disconnect()
    }

    companion object {
        private const val TAG = "SOCKET_DEBUG"
    }
}
