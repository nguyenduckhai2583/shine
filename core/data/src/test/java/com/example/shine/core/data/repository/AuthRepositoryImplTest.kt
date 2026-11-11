package com.example.shine.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.core.data.security.PasswordHasher
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.SignInStatus
import com.example.shine.core.network.AuthApi
import com.example.shine.core.network.dto.SessionDto
import com.example.shine.core.network.dto.SignInRequest
import com.example.shine.core.network.dto.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private lateinit var local: SessionLocalDataSource
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("auth_test_session.preferences_pb")
            },
            json,
        )
        fakeAuthApi = FakeAuthApi()
        repository = AuthRepositoryImpl(
            api = fakeAuthApi,
            local = local,
            passwordHasher = PasswordHasher(),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun signIn_success_savesSession() = runTest {
        val status = repository.signIn("user@test.com", "password").getOrThrow() as SignInStatus.Authenticated
        val session = status.session

        assertEquals("mock-token", session.token)
        assertEquals("mock-token", local.session.first()?.token)
    }

    @Test
    fun signIn_twoFactorRequired_returnsTwoFactorStatus() = runTest {
        fakeAuthApi.sessionToReturn = SessionDto(
            token = "temp-token",
            refreshToken = null,
            expireAt = null,
            user = null,
            isTmpToken = true,
        )

        val status = repository.signIn("user@test.com", "password").getOrThrow()
        assertTrue(status is SignInStatus.TwoFactorRequired)
        assertEquals("temp-token", (status as SignInStatus.TwoFactorRequired).tmpToken)
    }

    @Test
    fun signOut_clearsSession() = runTest {
        repository.signIn("user@test.com", "password").getOrThrow()
        assertEquals("mock-token", local.session.first()?.token)

        repository.signOut()

        assertNull(local.session.first())
    }

    @Test
    fun signOut_completesEvenIfCallingJobIsCancelled() = runTest(testDispatcher) {
        repository.signIn("user@test.com", "password").getOrThrow()

        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            repository.signOut()
        }
        job.cancel()
        job.join()

        assertNull(local.session.first())
    }

    private class FakeAuthApi : AuthApi {
        var sessionToReturn: SessionDto? = null

        override suspend fun signIn(request: SignInRequest): SessionDto {
            return sessionToReturn ?: SessionDto(
                token = "mock-token",
                refreshToken = "mock-refresh",
                expireAt = 123456789L,
                user = UserDto(id = "u-1", email = request.email, firstName = "Test", lastName = "User"),
            )
        }
    }
}
