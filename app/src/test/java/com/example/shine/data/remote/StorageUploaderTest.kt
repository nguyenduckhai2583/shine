package com.example.shine.data.remote

import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class StorageUploaderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var storageUploader: StorageUploader

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        storageUploader = StorageUploader(OkHttpClient())
    }

    @After
    fun tearDown() {
        mockWebServer.close()
    }

    @Test
    fun upload_success_postsMultipartToGivenUrl() {
        mockWebServer.enqueue(MockResponse(code = 204))

        val storageUrl = mockWebServer.url("/upload").toString()
        val fields = mapOf(
            "key" to "uploads/avatar.png",
            "policy" to "sample_policy",
        )

        storageUploader.upload(
            url = storageUrl,
            fields = fields,
            fileName = "avatar.png",
            mimeType = "image/png",
            bytes = okio.ByteString.of(1, 2, 3),
        )

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/upload", recordedRequest.url.encodedPath)

        val body = recordedRequest.body?.utf8().orEmpty()
        assertNotNull(body)
        assertTrue(body.contains("name=\"key\"\r\n\r\nuploads/avatar.png"))
        assertTrue(body.contains("name=\"policy\"\r\n\r\nsample_policy"))
        assertTrue(body.contains("filename=\"avatar.png\""))
    }

    @Test
    fun upload_serverError_throwsAppException() {
        mockWebServer.enqueue(MockResponse(code = 403))

        val storageUrl = mockWebServer.url("/upload").toString()

        try {
            storageUploader.upload(
                url = storageUrl,
                fields = emptyMap(),
                fileName = "avatar.png",
                mimeType = "image/png",
                bytes = okio.ByteString.of(1, 2, 3),
            )
            fail("Expected AppException")
        } catch (e: AppException) {
            assertEquals(AppErrorEnum.SERVER, e.type)
        }
    }
}
