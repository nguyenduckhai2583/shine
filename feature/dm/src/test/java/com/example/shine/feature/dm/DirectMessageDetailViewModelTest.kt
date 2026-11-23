package com.example.shine.feature.dm

import app.cash.turbine.test
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.testing.repository.FakeDirectMessageRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DirectMessageDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDirectMessageRepository()

    @Test
    fun load_success_showsDirectMessage() = runTest {
        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals("1", repository.lastId)
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dmNull_showsErrorUntilFetched() = runTest {
        repository.dmFlow.value = null
        repository.autoUpdateFlowOnGetDirectMessage = false

        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals(DirectMessageDetailUiState.Error(), awaitItem())

            repository.dmFlow.value = DM
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun offlineFirst_withCachedData_stillShowsDirectMessage() = runTest {
        repository.dmFlow.value = DM

        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private val DM = DirectMessage(
            id = "1",
            name = "Alice",
            time = null,
            type = "direct",
        )
    }
}
