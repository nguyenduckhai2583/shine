package com.example.shine.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
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
    fun getSession_whenEmpty_returnsNull() = runTest {
        val session = local.session.first()
        assertNull(session)
    }

    @Test
    fun save_andGetSession_returnsSaved() = runTest {
        val session = Session(
            token = "t1",
            refreshToken = "r1",
            expireAt = 1000L,
            workspaceId = "w1",
            user = User("u1", "u@h.com", "F", "L"),
        )
        local.save(session)

        val loaded = local.session.first()
        assertNotNull(loaded)
        assertEquals("t1", loaded?.token)
        assertEquals("r1", loaded?.refreshToken)
        assertEquals(1000L, loaded?.expireAt)
        assertEquals("w1", loaded?.workspaceId)
        assertEquals("u1", loaded?.user?.id)
    }

    @Test
    fun updateUser_updatesOnlyUser() = runTest {
        val session = Session(token = "t1", user = User("u1", "old@h.com"))
        local.save(session)

        val newUser = User("u1", "new@h.com", "First")
        local.updateUser(newUser)

        val loaded = local.session.first()
        assertEquals("new@h.com", loaded?.user?.email)
        assertEquals("t1", loaded?.token)
    }

    @Test
    fun updateAvatar_updatesAvatarUrl() = runTest {
        val session = Session(token = "t1", user = User("u1", "u@h.com", avatarUrl = "old.png"))
        local.save(session)

        local.updateAvatar("new.png")

        val loaded = local.session.first()
        assertEquals("new.png", loaded?.user?.avatarUrl)
        assertEquals("u@h.com", loaded?.user?.email)
    }

    @Test
    fun setWorkspaceId_updatesWorkspaceId() = runTest {
        val session = Session(token = "t1", workspaceId = "w1")
        local.save(session)

        local.setWorkspaceId("w2")

        val loaded = local.session.first()
        assertEquals("w2", loaded?.workspaceId)
        assertEquals("t1", loaded?.token)
    }

    @Test
    fun clear_removesSession() = runTest {
        val session = Session(token = "t1")
        local.save(session)

        local.clear()

        val loaded = local.session.first()
        assertNull(loaded)
    }
}
