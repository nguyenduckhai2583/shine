package com.example.shine.ui.camera

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class CameraMediaUtilsTest {

    @Test
    fun formatDuration_formatsSecondsCorrectly() {
        assertEquals("00:00", CameraMediaUtils.formatDuration(0))
        assertEquals("00:05", CameraMediaUtils.formatDuration(5))
        assertEquals("00:59", CameraMediaUtils.formatDuration(59))
        assertEquals("01:00", CameraMediaUtils.formatDuration(60))
        assertEquals("01:23", CameraMediaUtils.formatDuration(83))
        assertEquals("10:05", CameraMediaUtils.formatDuration(605))
    }

    @Test
    fun capturedMedia_photoPreservesUri() {
        val uri = mock(Uri::class.java)
        val photo: CapturedMedia = CapturedMedia.Photo(uri)

        assertEquals(uri, photo.uri)
    }

    @Test
    fun capturedMedia_videoPreservesUriAndDuration() {
        val uri = mock(Uri::class.java)
        val video = CapturedMedia.Video(uri = uri, durationMs = 15000L)

        assertEquals(uri, video.uri)
        assertEquals(15000L, video.durationMs)
    }

    @Test
    fun cameraCaptureModes_containExpectedValues() {
        val modes = CameraCaptureMode.entries
        assertTrue(modes.contains(CameraCaptureMode.PHOTO))
        assertTrue(modes.contains(CameraCaptureMode.VIDEO))
        assertEquals(2, modes.size)
    }

}
