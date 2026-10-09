package com.example.shine.ui.planix

import androidx.paging.PagingData
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
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
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun onSearchQueryChanged_updatesSearchQuery() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.onSearchQueryChanged("Test")
        assertEquals("Test", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun projectsPagingFlow_initialLoadQueriesRepositoryWithNull() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.projectsPagingFlow.collect {}
        }
        testScheduler.advanceUntilIdle()

        assertEquals(null, planixRepository.lastSearchKey)
    }

    @Test
    fun projectsPagingFlow_debouncesAndQueriesRepositoryWithKeyword() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.projectsPagingFlow.collect {}
        }
        testScheduler.advanceUntilIdle()
        assertEquals(null, planixRepository.lastSearchKey)

        viewModel.onSearchQueryChanged("shine")
        testScheduler.advanceTimeBy(350)
        testScheduler.runCurrent()

        assertEquals("shine", planixRepository.lastSearchKey)
    }

    private class FakePlanixRepository : PlanixRepository {
        var lastSearchKey: String? = "UNSET"

        override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
            lastSearchKey = searchKey
            return emptyFlow()
        }
    }
}
