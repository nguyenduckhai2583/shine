package com.example.shine.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    val session: Flow<Session?> = dataStore.data.map { prefs ->
        getSession(prefs)
    }

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_SESSION] = json.encodeToString(session)
        }
    }

    suspend fun updateUser(user: User) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(user = user)
            prefs[KEY_SESSION] = json.encodeToString(updated)
        }
    }

    suspend fun updateAvatar(avatarUrl: String) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(
                user = current.user?.copy(avatarUrl = avatarUrl),
            )
            prefs[KEY_SESSION] = json.encodeToString(updated)
        }
    }

    suspend fun setWorkspaceId(workspaceId: String) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(workspaceId = workspaceId)
            prefs[KEY_SESSION] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun getSession(prefs: Preferences): Session? {
        val rawSession = prefs[KEY_SESSION] ?: return null
        return try {
            json.decodeFromString<Session>(rawSession)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session")
    }
}
