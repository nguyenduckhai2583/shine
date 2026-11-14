package com.example.shine.feature.channel

import com.example.shine.core.common.R
import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.model.Channel
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.common.ui.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelDetailViewModelTest {

    private val repository = FakeChannelRepository()
    private val shortcutManager = FakeAppShortcutManager()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_success_showsChannelAndUpdatesShortcut() = runTest {
        repository.error = null

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals("1", repository.lastId)
        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
        assertEquals("general", shortcutManager.lastUpdatedChannelName)
    }

    @Test
    fun deeplink_initialRoomDataNull_showsLoadingUntilFetched() = runTest {
        // Giả lập mở từ Deeplink: Room DB ban đầu chưa có data
        repository.channelFlow.value = null
        repository.autoUpdateFlowOnGetChannel = false

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Room DB chưa có data và chưa có lỗi -> Phải hiển thị Loading
        assertEquals(ChannelDetailUiState.Loading, viewModel.uiState.value)
        assertNull(shortcutManager.lastUpdatedChannelId)

        // Sau đó API getChannel hoặc Home getChannels ghi vào Room DB thành công
        repository.channelFlow.value = CHANNEL

        // State lập tức chuyển sang Success và shortcut được update qua onFirst
        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
        assertEquals("general", shortcutManager.lastUpdatedChannelName)
    }

    @Test
    fun load_failure_showsErrorThenRetrySucceeds() = runTest {
        repository.error = AppException(AppErrorEnum.NO_INTERNET)
        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(
            ChannelDetailUiState.Error(UiText.Resource(R.string.error_no_internet)),
            viewModel.uiState.value,
        )
        assertNull(shortcutManager.lastUpdatedChannelId)

        repository.error = null
        viewModel.load()

        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
    }

    @Test
    fun offlineFirst_withCachedData_syncFailure_stillShowsChannel() = runTest {
        // Giả lập Local DB đã có sẵn dữ liệu của channel
        repository.channelFlow.value = CHANNEL
        // Giả lập mạng bị lỗi khi sync
        repository.error = AppException(AppErrorEnum.NO_INTERNET)

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Offline first: Vẫn hiển thị nội dung kênh đã cache mà không bị báo lỗi chặn màn hình
        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
    }

    private class FakeChannelRepository : ChannelRepository {
        var error: AppException? = null
        var lastId: String? = null
        var autoUpdateFlowOnGetChannel = true
        val channelFlow = MutableStateFlow<Channel?>(null)

        override suspend fun getChannels(): Result<Unit> = Result.success(Unit)

        override suspend fun getChannel(id: String): Result<Unit> {
            lastId = id
            val currentError = error
            return if (currentError != null) {
                Result.failure(currentError)
            } else {
                if (autoUpdateFlowOnGetChannel) {
                    channelFlow.value = CHANNEL
                }
                Result.success(Unit)
            }
        }

        override fun getChannelFlow(id: String): Flow<Channel?> = channelFlow

        override suspend fun createChannel(name: String): Result<Unit> = Result.success(Unit)
    }

    private companion object {
        val CHANNEL = Channel("1", "general",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = 1L,
            lastActivityAt = 2L
        )
    }
}
