package com.example.shine.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[TOKEN] ?: return@map null
        Session(
            token = token,
            refreshToken = prefs[REFRESH_TOKEN],
            expireAt = prefs[EXPIRE_AT],
            user = prefs[USER_ID]?.let { id ->
                User(
                    id = id,
                    email = prefs[USER_EMAIL],
                    firstName = prefs[USER_FIRST_NAME],
                    lastName = prefs[USER_LAST_NAME],
                )
            },
            workspaceId = prefs[WORKSPACE_ID],
        )
    }

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[TOKEN] = session.token
            session.refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            session.expireAt?.let { prefs[EXPIRE_AT] = it }
            session.workspaceId?.let { prefs[WORKSPACE_ID] = it }
            session.user?.let { user ->
                prefs[USER_ID] = user.id
                user.email?.let { prefs[USER_EMAIL] = it }
                user.firstName?.let { prefs[USER_FIRST_NAME] = it }
                user.lastName?.let { prefs[USER_LAST_NAME] = it }
            }
        }
    }

    suspend fun setWorkspaceId(workspaceId: String) {
        dataStore.edit { prefs ->
            prefs[WORKSPACE_ID] = workspaceId
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRE_AT = longPreferencesKey("expire_at")
        val WORKSPACE_ID = stringPreferencesKey("workspace_id")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_FIRST_NAME = stringPreferencesKey("user_first_name")
        val USER_LAST_NAME = stringPreferencesKey("user_last_name")
    }
}
