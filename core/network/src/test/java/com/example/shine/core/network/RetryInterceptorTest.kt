package com.example.shine.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private val server = MockWebServer()
    private val recordedDelays = mutableListOf<Long>()
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        recordedDelays.clear()
        val interceptor = RetryInterceptor(
            maxRetries = 3,
            retryDelays = listOf(1000L, 3000L, 5000L),
            sleeper = { delayMs -> recordedDelays.add(delayMs) },
        )
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `does not retry on successful 200 response`() {
        server.enqueue(MockResponse(code = 200, body = "OK"))

        val response = client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
        assertEquals(emptyList<Long>(), recordedDelays)
    }

    @Test
    fun `does not retry on 429 Too Many Requests`() {
        server.enqueue(MockResponse(code = 429, body = "Rate limited"))

        val response = client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        assertEquals(429, response.code)
        assertEquals(1, server.requestCount)
        assertEquals(emptyList<Long>(), recordedDelays)
    }

    @Test
    fun `does not retry on 400, 401, 403, 404 client errors`() {
        for (code in listOf(400, 401, 403, 404, 422)) {
            server.enqueue(MockResponse(code = code))

            val response = client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

            assertEquals(code, response.code)
        }
        assertEquals(5, server.requestCount)
        assertEquals(emptyList<Long>(), recordedDelays)
    }

    @Test
    fun `retries on 500, 502, 503, 504 and succeeds when subsequent attempt returns 200`() {
        server.enqueue(MockResponse(code = 500))
        server.enqueue(MockResponse(code = 502))
        server.enqueue(MockResponse(code = 200, body = "Success"))

        val response = client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(3, server.requestCount)
        assertEquals(listOf(1000L, 3000L), recordedDelays)
    }

    @Test
    fun `retries up to 3 times on continuous 503 and returns the last failure`() {
        server.enqueue(MockResponse(code = 503))
        server.enqueue(MockResponse(code = 503))
        server.enqueue(MockResponse(code = 503))
        server.enqueue(MockResponse(code = 503))

        val response = client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        assertEquals(503, response.code)
        // 1 initial request + 3 retries = 4 total requests
        assertEquals(4, server.requestCount)
        assertEquals(listOf(1000L, 3000L, 5000L), recordedDelays)
    }
}
