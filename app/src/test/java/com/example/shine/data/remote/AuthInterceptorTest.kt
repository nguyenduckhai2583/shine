package com.example.shine.data.remote

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.TokenProvider
import com.example.shine.domain.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var local: SessionLocalDataSource
    private lateinit var client: OkHttpClient

    private val refreshCalls = AtomicInteger()
    private var refreshResponse = MockResponse(code = 200, body = """{"token":"new","refreshToken":"r2"}""")
    private var validToken = "new"

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.url.encodedPath.endsWith("auth/refresh-token") -> {
                    refreshCalls.incrementAndGet()
                    refreshResponse
                }
                request.headers["Authorization"] == null -> MockResponse(code = 200)
                request.headers["Authorization"] == "Bearer $validToken" -> MockResponse(code = 200)
                request.headers["Authorization"] == "Bearer revoked" -> MockResponse(code = 401)
                else -> MockResponse(code = 440)
            }
        }
        server.start()

        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("session.preferences_pb")
            },
        )
        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TokenRefreshApi::class.java)
        client = OkHttpClient.Builder()
            .addInterceptor(
                AuthInterceptor(TokenProvider(local, scope), TokenRefresher(refreshApi, local, json)),
            )
            .build()
    }

    @After
    fun tearDown() {
        server.close()
        scope.cancel()
    }

    @Test
    fun noSession_sendsRequestWithoutAuthorization() {
        assertEquals(200, call())
        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun validToken_addsBearerHeader() {
        saveSession(token = "new")

        assertEquals(200, call())
        assertEquals("Bearer new", server.takeRequest().headers["Authorization"])
        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun expiredToken_refreshesSavesAndRetries() {
        saveSession(token = "old")

        assertEquals(200, call())
        assertEquals(1, refreshCalls.get())
        val saved = currentSession()
        assertEquals("new", saved?.token)
        assertEquals("r2", saved?.refreshToken)
    }

    @Test
    fun refreshResponseWrappedInData_isAlsoParsed() {
        refreshResponse = MockResponse(code = 200, body = """{"data":{"token":"new"}}""")
        saveSession(token = "old")

        assertEquals(200, call())
        assertEquals("r1", currentSession()?.refreshToken)
    }

    @Test
    fun refreshRejectedWith401_signsOut() {
        refreshResponse = MockResponse(code = 401)
        saveSession(token = "old")

        assertEquals(440, call())
        assertNull(currentSession())
    }

    @Test
    fun refreshServerError_keepsSession() {
        refreshResponse = MockResponse(code = 500)
        saveSession(token = "old")

        assertEquals(440, call())
        assertEquals("old", currentSession()?.token)
    }

    @Test
    fun unauthorized_signsOut() {
        saveSession(token = "revoked")

        assertEquals(401, call())
        assertNull(currentSession())
    }

    @Test
    fun concurrentExpiredRequests_refreshOnlyOnce() {
        saveSession(token = "old")
        val executor = Executors.newFixedThreadPool(5)

        val codes = (1..5).map { executor.submit<Int> { call() } }.map { it.get() }

        executor.shutdown()
        assertEquals(List(5) { 200 }, codes)
        assertEquals(1, refreshCalls.get())
    }

    private fun call(): Int =
        client.newCall(Request(server.url("/api"))).execute().use { it.code }

    private fun saveSession(token: String) = runBlocking {
        local.save(Session(token = token, refreshToken = "r1", expireAt = null, user = null))
    }

    private fun currentSession() = runBlocking { local.session.first() }
}
