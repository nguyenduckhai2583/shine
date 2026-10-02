package com.example.shine.data.local

import com.example.shine.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the latest token in memory so OkHttp can read it synchronously. */
@Singleton
class TokenProvider @Inject constructor(
    private val local: SessionLocalDataSource,
    @ApplicationScope scope: CoroutineScope,
) {
    private val token: StateFlow<String?> = local.session
        .map { it?.token }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Falls back to DataStore while the cache is still empty, e.g. the first request after launch. */
    fun current(): String? = token.value ?: runBlocking { local.session.first()?.token }
}
