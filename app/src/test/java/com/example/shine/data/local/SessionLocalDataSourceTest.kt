package com.example.shine.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionLocalDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private lateinit var local: SessionLocalDataSource

    @Before
    fun setUp() {
        val testFile = tempFolder.newFile("test_session.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
        local = SessionLocalDataSource(dataStore, json)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun save_storesWholeSessionObject() = runTest {
        val user = User(
            id = "user-123",
            email = "user@test.com",
            firstName = "John",
            lastName = "Doe",
            avatarUrl = "https://cdn.example.com/avatar.png",
        )
        val session = Session(
            token = "jwt-token-abc",
            refreshToken = "refresh-token-xyz",
            expireAt = 123456789L,
            user = user,
            workspaceId = "ws-999",
        )

        local.save(session)

        val retrieved = local.session.first()
        assertNotNull(retrieved)
        assertEquals("jwt-token-abc", retrieved?.token)
        assertEquals("refresh-token-xyz", retrieved?.refreshToken)
        assertEquals(123456789L, retrieved?.expireAt)
        assertEquals("ws-999", retrieved?.workspaceId)
        assertEquals(user, retrieved?.user)
    }

    @Test
    fun updateUser_updatesExistingUserInSession() = runTest {
        val session = Session(
            token = "token",
            user = User(id = "user-1", email = "old@test.com", firstName = "Old", lastName = "Name"),
        )
        local.save(session)

        val updatedUser = User(id = "user-1", email = "new@test.com", firstName = "New", lastName = "Name", avatarUrl = "url")
        local.updateUser(updatedUser)

        val retrieved = local.session.first()
        assertEquals(updatedUser, retrieved?.user)
        assertEquals("token", retrieved?.token)
    }

    @Test
    fun updateAvatar_updatesUserAvatarUrl() = runTest {
        val session = Session(
            token = "token",
            user = User(id = "user-1", email = "user@test.com", firstName = "John", lastName = "Doe", avatarUrl = null),
        )
        local.save(session)

        local.updateAvatar("https://cdn.example.com/new.png")

        val retrieved = local.session.first()
        assertEquals("https://cdn.example.com/new.png", retrieved?.user?.avatarUrl)
        assertEquals("John", retrieved?.user?.firstName)
    }

    @Test
    fun setWorkspaceId_updatesWorkspaceIdInSession() = runTest {
        val session = Session(token = "token", workspaceId = null)
        local.save(session)

        local.setWorkspaceId("ws-new")

        val retrieved = local.session.first()
        assertEquals("ws-new", retrieved?.workspaceId)
        assertEquals("token", retrieved?.token)
    }

    @Test
    fun clear_removesSession() = runTest {
        val session = Session(token = "token")
        local.save(session)

        local.clear()

        val retrieved = local.session.first()
        assertNull(retrieved)
    }
}
