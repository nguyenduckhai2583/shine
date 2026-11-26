package com.example.shine.feature.planix

import app.cash.turbine.test
import com.example.shine.core.testing.repository.FakePlanixRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanixListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val planixRepository = FakePlanixRepository()

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

            viewModel.onSearchQueryChanged("  shine  ")
            testScheduler.advanceTimeBy(350)
            testScheduler.runCurrent()

            awaitItem()
            assertEquals("shine", planixRepository.lastSearchKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun projectsPagingFlow_clearingQueryImmediatelyQueriesRepositoryWithNull() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.projectsPagingFlow.test {
            awaitItem()

            viewModel.onSearchQueryChanged("shine")
            testScheduler.advanceTimeBy(350)
            testScheduler.runCurrent()
            awaitItem()
            assertEquals("shine", planixRepository.lastSearchKey)

            viewModel.onSearchQueryChanged("")
            testScheduler.runCurrent()
            awaitItem()
            assertEquals(null, planixRepository.lastSearchKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun projectsPagingFlow_blankQueryQueriesRepositoryWithNull() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        viewModel.projectsPagingFlow.test {
            awaitItem()

            viewModel.onSearchQueryChanged("   ")
            testScheduler.advanceTimeBy(350)
            testScheduler.runCurrent()

            awaitItem()
            assertEquals(null, planixRepository.lastSearchKey)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
