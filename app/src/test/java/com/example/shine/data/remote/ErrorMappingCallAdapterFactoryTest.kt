package com.example.shine.data.remote

import com.example.shine.domain.model.AppErrorEnum
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

        assertEquals(AppErrorEnum.SERVER, error.type)
        assertEquals("Wrong password", error.errorMessage)
        assertEquals(400, error.statusCode)
    }

    @Test
    fun httpErrorWithoutBody_mapsToServerWithoutMessage() = runBlocking {
        server.enqueue(MockResponse(code = 500))

        val error = captureError { api.item() }

        assertEquals(AppErrorEnum.SERVER, error.type)
        assertNull(error.errorMessage)
        assertEquals(500, error.statusCode)
    }

    @Test
    fun connectionFailure_mapsToNetwork() = runBlocking {
        server.enqueue(
            MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build(),
        )

        assertEquals(AppErrorEnum.NO_INTERNET, captureError { api.item() }.type)
    }

    @Test
    fun malformedBody_mapsToUnknown() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = "not json"))

        assertEquals(AppErrorEnum.UNKNOWN, captureError { api.item() }.type)
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
