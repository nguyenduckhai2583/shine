package com.example.shine.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shine.core.datastore.security.CryptoManager
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val cryptoManager: CryptoManager,
) {
    val session: Flow<Session?> = dataStore.data.map { prefs ->
        getSession(prefs)
    }

    suspend fun save(session: Session) {
        val rawJson = json.encodeToString(session)
        val encrypted = cryptoManager.encrypt(rawJson)
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_SESSION] = encrypted
        }
    }

    suspend fun updateUser(user: User) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(user = user)
            val rawJson = json.encodeToString(updated)
            prefs[KEY_SESSION] = cryptoManager.encrypt(rawJson)
        }
    }

    suspend fun updateAvatar(avatarUrl: String) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(
                user = current.user?.copy(avatarUrl = avatarUrl),
            )
            val rawJson = json.encodeToString(updated)
            prefs[KEY_SESSION] = cryptoManager.encrypt(rawJson)
        }
    }

    suspend fun setWorkspaceId(workspaceId: String) {
        dataStore.edit { prefs ->
            val current = getSession(prefs) ?: return@edit
            val updated = current.copy(workspaceId = workspaceId)
            val rawJson = json.encodeToString(updated)
            prefs[KEY_SESSION] = cryptoManager.encrypt(rawJson)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun getSession(prefs: Preferences): Session? {
        val rawSession = prefs[KEY_SESSION] ?: return null
        return try {
            val decryptedJson = cryptoManager.decrypt(rawSession)
            json.decodeFromString<Session>(decryptedJson)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session")
    }
}
