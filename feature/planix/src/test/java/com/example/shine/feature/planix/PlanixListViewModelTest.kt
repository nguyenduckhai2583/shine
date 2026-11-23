package com.example.shine.feature.planix

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanixListViewModelTest {

    private val planixRepository = FakePlanixRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_initialStateHasEmptySearchQuery() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.uiState.test {
            assertEquals("", awaitItem().searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSearchQueryChanged_updatesSearchQuery() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSearchQueryChanged("Test")
            assertEquals("Test", awaitItem().searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun projectsPagingFlow_initialLoadQueriesRepositoryWithNull() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.projectsPagingFlow.test {
            awaitItem()
            assertEquals(null, planixRepository.lastSearchKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun projectsPagingFlow_debouncesAndQueriesRepositoryWithKeyword() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.projectsPagingFlow.test {
            awaitItem()
            assertEquals(null, planixRepository.lastSearchKey)

            viewModel.onSearchQueryChanged("shine")
            testScheduler.advanceTimeBy(350)
            testScheduler.runCurrent()

            awaitItem()
            assertEquals("shine", planixRepository.lastSearchKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePlanixRepository : PlanixRepository {
        var lastSearchKey: String? = "UNSET"

        override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
            lastSearchKey = searchKey
            return flowOf(PagingData.empty())
        }

        override fun getProjectById(id: String): Flow<Project?> {
            return emptyFlow()
        }
    }
}
