package com.example.shine.data.remote

import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class ErrorMappingCallAdapterFactoryTest {

    @Serializable
    data class Item(val name: String)

    interface TestApi {
        @GET("item")
        suspend fun item(): Item
    }

    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var api: TestApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ErrorMappingCallAdapterFactory(json))
            .build()
            .create(TestApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun success_returnsBody() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = """{"name":"general"}"""))

        assertEquals(Item("general"), api.item())
    }

    @Test
    fun httpError_mapsToServerWithBackendMessage() = runBlocking {
        server.enqueue(MockResponse(code = 400, body = """{"message":"Wrong password"}"""))

        val error = captureError { api.item() }

        assertEquals(AppError.SERVER, error.error)
        assertEquals("Wrong password", error.serverMessage)
    }

    @Test
    fun httpErrorWithoutBody_mapsToServerWithoutMessage() = runBlocking {
        server.enqueue(MockResponse(code = 500))

        val error = captureError { api.item() }

        assertEquals(AppError.SERVER, error.error)
        assertNull(error.serverMessage)
    }

    @Test
    fun connectionFailure_mapsToNetwork() = runBlocking {
        server.enqueue(
            MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build(),
        )

        assertEquals(AppError.NO_INTERNET, captureError { api.item() }.error)
    }

    @Test
    fun malformedBody_mapsToUnknown() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = "not json"))

        assertEquals(AppError.UNKNOWN, captureError { api.item() }.error)
    }

    private suspend fun captureError(block: suspend () -> Unit): AppException {
        try {
            block()
        } catch (e: AppException) {
            return e
        }
        fail("Expected an exception")
        error("unreachable")
    }
}
