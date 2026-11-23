package com.example.shine.feature.planix

import app.cash.turbine.test
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.model.Project
import com.example.shine.core.testing.repository.FakePlanixRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlanixDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val planixRepository = FakePlanixRepository()

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
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is PlanixDetailUiState.Success)
            assertEquals(expectedProject, (state as PlanixDetailUiState.Success).project)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenProjectNotFound_emitsError() = runTest {
        planixRepository.projectFlow.value = null

        val viewModel = PlanixDetailViewModel("proj-999", planixRepository)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is PlanixDetailUiState.Error)
            assertEquals(
                UiText.Resource(R.string.error_project_not_found),
                (state as PlanixDetailUiState.Error).message,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
