package com.example.shine.feature.planix

import androidx.paging.PagingData
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanixDetailViewModelTest {

    private val planixRepository = FakePlanixRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun uiState_whenProjectFound_emitsSuccess() = runTest {
        val expectedProject = Project(
            id = "proj-1",
            name = "Mobile App Redesign",
            prefix = "MAR",
            status = "ACTIVE",
            logoUrl = null,
            leaderName = "John Doe",
        )
        planixRepository.projectFlow.value = expectedProject

        val viewModel = PlanixDetailViewModel("proj-1", planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertTrue(state is PlanixDetailUiState.Success)
        assertEquals(expectedProject, (state as PlanixDetailUiState.Success).project)
    }

    @Test
    fun uiState_whenProjectNotFound_emitsError() = runTest {
        planixRepository.projectFlow.value = null

        val viewModel = PlanixDetailViewModel("proj-999", planixRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertTrue(state is PlanixDetailUiState.Error)
        assertEquals(
            UiText.Resource(R.string.error_project_not_found),
            (state as PlanixDetailUiState.Error).message,
        )
    }

    private class FakePlanixRepository : PlanixRepository {
        val projectFlow = MutableStateFlow<Project?>(null)

        override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
            return emptyFlow()
        }

        override fun getProjectById(id: String): Flow<Project?> {
            return projectFlow
        }
    }
}
