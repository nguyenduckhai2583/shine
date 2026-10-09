package com.example.shine.ui.planix

import com.example.shine.R
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import com.example.shine.ui.common.UiText
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
import org.junit.Assert.assertFalse
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
    fun init_observesProjectsFromFlowAndLoadsRemote() = runTest {
        val viewModel = PlanixListViewModel(planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.projects.size)
        assertEquals("Shine App", state.projects[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadProjects_error_updatesErrorMessage() = runTest {
        planixRepository.shouldFail = true
        planixRepository.projectsFlow.value = emptyList()

        val viewModel = PlanixListViewModel(planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.projects.size)
        assertEquals(UiText.Resource(R.string.error_no_internet), state.errorMessage)
        assertFalse(state.isLoading)
    }

    private class FakePlanixRepository : PlanixRepository {
        val projectsFlow = MutableStateFlow(
            listOf(Project("1", "Shine App", "SA", "ACTIVE", null, "John"))
        )
        var shouldFail = false

        override fun getProjectsFlow(): Flow<List<Project>> = projectsFlow

        override suspend fun getProjects(searchKey: String?, status: String?): List<Project> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return projectsFlow.value
        }
    }
}
