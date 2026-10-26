package com.example.shine.data.repository

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.ContentUriResolver
import com.example.shine.data.local.ResolvedFile
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.StorageUploader
import com.example.shine.data.remote.UserApi
import com.example.shine.data.remote.dto.DraftMediaRequest
import com.example.shine.data.remote.dto.DraftMediaResponse
import com.example.shine.data.remote.dto.FileDto
import com.example.shine.data.remote.dto.PresignedPostDto
import com.example.shine.data.remote.dto.UpdateUserRequest
import com.example.shine.data.remote.dto.UserProfileDto
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var mockWebServer: MockWebServer
    private lateinit var local: SessionLocalDataSource
    private lateinit var fakeApi: FakeUserApi
    private lateinit var contentUriResolver: ContentUriResolver
    private lateinit var storageUploader: StorageUploader
    private lateinit var repository: UserRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("test_session.preferences_pb")
            },
            json,
        )
        fakeApi = FakeUserApi()
        contentUriResolver = FakeContentUriResolver()
        storageUploader = StorageUploader(OkHttpClient())

        repository = UserRepositoryImpl(
            contentUriResolver = contentUriResolver,
            storageUploader = storageUploader,
            userApi = fakeApi,
            local = local,
            json = json,
        )
    }

    @After
    fun tearDown() {
        mockWebServer.close()
        scope.cancel()
    }

    @Test
    fun updateAvatar_success_uploadsToS3UpdatesProfileAndPersistsLocally() = runTest {
        local.save(
            Session(
                token = "token",
                refreshToken = null,
                expireAt = null,
                user = com.example.shine.domain.model.User(
                    id = "user-1",
                    email = "user@example.com",
                    firstName = "John",
                    lastName = "Doe",
                ),
            ),
        )

        val s3Url = mockWebServer.url("/").toString()
        fakeApi.draftResponse = DraftMediaResponse(
            id = "avatar-123",
            name = "test.png",
            path = "images/avatar-123.png",
            presignedPost = PresignedPostDto(
                url = s3Url,
                fields = """{"x-amz-meta-id":"avatar-123","bucket":"test-bucket"}""",
            ),
            presignedUrl = "https://cdn.example.com/avatar-123.png",
        )

        fakeApi.currentProfile = UserProfileDto(
            id = "user-1",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            avatarId = null,
        )

        fakeApi.updatedProfile = UserProfileDto(
            id = "user-1",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            avatarId = "avatar-123",
            avatar = FileDto(signedUrl = "https://cdn.example.com/signed-avatar-123.png"),
        )

        // Mock S3 response (204 No Content)
        mockWebServer.enqueue(MockResponse(code = 204))

        val resultUser = repository.updateAvatar(org.mockito.Mockito.mock(Uri::class.java))

        // Verify S3 request
        val recordedS3Request = mockWebServer.takeRequest()
        assertEquals("POST", recordedS3Request.method)
        val bodyContent = recordedS3Request.body?.utf8().orEmpty()
        assertNotNull(bodyContent)
        assert(bodyContent.contains("avatar-123"))

        // Verify API calls
        assertEquals(0, fakeApi.getProfileCalls)
        assertEquals("avatar-123", fakeApi.lastUpdateUserRequest?.avatarId)
        assertEquals("John", fakeApi.lastUpdateUserRequest?.firstName)

        // Verify result
        assertEquals("https://cdn.example.com/signed-avatar-123.png", resultUser.avatarUrl)

        // Verify local update
        val updatedSession = local.session.first()
        assertEquals("https://cdn.example.com/signed-avatar-123.png", updatedSession?.user?.avatarUrl)
    }

    @Test
    fun updateAvatar_s3Fails_throwsException() = runTest {
        val s3Url = mockWebServer.url("/").toString()
        fakeApi.draftResponse = DraftMediaResponse(
            id = "avatar-123",
            presignedPost = PresignedPostDto(
                url = s3Url,
                fields = "{}",
            ),
        )

        // S3 returns 403 Forbidden
        mockWebServer.enqueue(MockResponse(code = 403))

        try {
            repository.updateAvatar(org.mockito.Mockito.mock(Uri::class.java))
            org.junit.Assert.fail("Expected AppException")
        } catch (e: AppException) {
            // Expected
        }
    }

    @Test
    fun getProfile_success_updatesLocalDataSourceAndReturnsUser() = runTest {
        local.save(
            Session(
                token = "token",
                user = com.example.shine.domain.model.User(
                    id = "user-1",
                    email = "old@example.com",
                    firstName = "Old",
                    lastName = "Name",
                ),
            ),
        )

        fakeApi.currentProfile = UserProfileDto(
            id = "user-1",
            email = "new@example.com",
            firstName = "New",
            lastName = "Name",
            avatar = FileDto(signedUrl = "https://cdn.example.com/fresh-avatar.png"),
        )

        val result = repository.getProfile()

        assertEquals("user-1", result.id)
        assertEquals("new@example.com", result.email)
        assertEquals("New", result.firstName)
        assertEquals("https://cdn.example.com/fresh-avatar.png", result.avatarUrl)

        val updatedLocalSession = local.session.first()
        assertEquals("new@example.com", updatedLocalSession?.user?.email)
        assertEquals("New", updatedLocalSession?.user?.firstName)
        assertEquals("https://cdn.example.com/fresh-avatar.png", updatedLocalSession?.user?.avatarUrl)
    }

    private class FakeContentUriResolver : ContentUriResolver(org.mockito.Mockito.mock(android.content.Context::class.java)) {
        var resolvedFile = ResolvedFile("test.png", "image/png", okio.ByteString.of(1, 2, 3, 4))
        override fun resolve(uri: Uri): ResolvedFile = resolvedFile
    }

    private class FakeUserApi : UserApi {
        var draftResponse: DraftMediaResponse? = null
        var currentProfile: UserProfileDto? = null
        var updatedProfile: UserProfileDto? = null
        var lastUpdateUserRequest: UpdateUserRequest? = null
        var getProfileCalls = 0

        override suspend fun getProfile(): UserProfileDto {
            getProfileCalls++
            return currentProfile ?: throw IllegalStateException("currentProfile not set")
        }

        override suspend fun createAvatarDraft(request: DraftMediaRequest): DraftMediaResponse {
            return draftResponse ?: throw IllegalStateException("draftResponse not set")
        }

        override suspend fun updateProfile(request: UpdateUserRequest): UserProfileDto {
            lastUpdateUserRequest = request
            return updatedProfile ?: throw IllegalStateException("updatedProfile not set")
        }
    }
}
