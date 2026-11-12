package com.example.shine.feature.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraViewModel(testDispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun defaultState_isInitializedCorrectly() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(CameraCaptureMode.PHOTO, state.captureMode)
        assertEquals(CameraLens.BACK, state.currentLens)
        assertFalse(state.isRecording)
        assertEquals(0, state.recordingDurationSeconds)
        assertTrue(state.hasAudioPermission)
    }

    @Test
    fun initialize_setsAllowedModesAndAudioPermission() = runTest(testDispatcher) {
        viewModel.initialize(
            allowedModes = setOf(CameraCaptureMode.VIDEO),
            hasAudioPermission = false,
        )

        val state = viewModel.uiState.value
        assertEquals(CameraCaptureMode.VIDEO, state.captureMode)
        assertEquals(setOf(CameraCaptureMode.VIDEO), state.allowedModes)
        assertFalse(state.hasAudioPermission)
    }

    @Test
    fun onModeSelected_updatesCaptureMode_whenAllowed() = runTest(testDispatcher) {
        viewModel.initialize(
            allowedModes = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
            hasAudioPermission = true,
        )

        viewModel.onModeSelected(CameraCaptureMode.VIDEO)
        assertEquals(CameraCaptureMode.VIDEO, viewModel.uiState.value.captureMode)

        viewModel.onModeSelected(CameraCaptureMode.PHOTO)
        assertEquals(CameraCaptureMode.PHOTO, viewModel.uiState.value.captureMode)
    }

    @Test
    fun onFlipCamera_cyclesThroughAvailableLenses() = runTest(testDispatcher) {
        viewModel.setAvailableLenses(listOf(CameraLens.BACK, CameraLens.FRONT))
        assertEquals(CameraLens.BACK, viewModel.uiState.value.currentLens)

        viewModel.onFlipCamera()
        assertEquals(CameraLens.FRONT, viewModel.uiState.value.currentLens)

        viewModel.onFlipCamera()
        assertEquals(CameraLens.BACK, viewModel.uiState.value.currentLens)
    }

    @Test
    fun modeChangeAndFlip_areIgnoredWhileRecording() = runTest(testDispatcher) {
        viewModel.setAvailableLenses(listOf(CameraLens.BACK, CameraLens.FRONT))
        viewModel.onModeSelected(CameraCaptureMode.VIDEO)

        viewModel.onRecordingStarted()
        runCurrent()
        assertTrue(viewModel.uiState.value.isRecording)

        viewModel.onFlipCamera()
        runCurrent()
        assertEquals(CameraLens.BACK, viewModel.uiState.value.currentLens)

        viewModel.onModeSelected(CameraCaptureMode.PHOTO)
        runCurrent()
        assertEquals(CameraCaptureMode.VIDEO, viewModel.uiState.value.captureMode)

        viewModel.onRecordingStopped()
        runCurrent()
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun recording_timerIncrementsEverySecond() = runTest(testDispatcher) {
        viewModel.onRecordingStarted()
        runCurrent()
        assertEquals(0, viewModel.uiState.value.recordingDurationSeconds)
        assertTrue(viewModel.uiState.value.isRecording)

        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.recordingDurationSeconds)

        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(3, viewModel.uiState.value.recordingDurationSeconds)

        viewModel.onRecordingStopped()
        runCurrent()
        assertFalse(viewModel.uiState.value.isRecording)
    }
}
