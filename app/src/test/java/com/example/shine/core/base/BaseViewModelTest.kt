package com.example.shine.core.base

import com.example.shine.ui.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class BaseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestViewModel : BaseViewModel() {
        fun runSuccessTask(value: String, callback: (String) -> Unit) {
            execute(
                task = { Result.success(value) },
                onSuccess = callback,
            )
        }

        fun runFailureTask(errorMessage: String, errorCallback: (UiText) -> Unit, finallyCallback: () -> Unit) {
            execute(
                task = { Result.failure<String>(Exception(errorMessage)) },
                onError = errorCallback,
                onFinally = finallyCallback,
            )
        }
    }

    @Test
    fun execute_success_callsOnSuccess() = runTest {
        val viewModel = TestViewModel()
        var result = ""

        viewModel.runSuccessTask("ok") {
            result = it
        }

        assertEquals("ok", result)
    }

    @Test
    fun execute_failure_callsOnErrorAndOnFinally() = runTest {
        val viewModel = TestViewModel()
        var capturedError: UiText? = null
        var finallyRan = false

        viewModel.runFailureTask(
            errorMessage = "Server down",
            errorCallback = { capturedError = it },
            finallyCallback = { finallyRan = true },
        )

        assertEquals(UiText("Server down"), capturedError)
        assertTrue(finallyRan)
    }
}
