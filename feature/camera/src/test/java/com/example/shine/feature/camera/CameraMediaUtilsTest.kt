package com.example.shine.feature.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val uriString = "content://media/external/images/media/1"
        val photo: CapturedMedia = CapturedMedia.Photo(uriString)

        assertEquals(uriString, photo.uri)
    }

    @Test
    fun capturedMedia_videoPreservesUriAndDuration() {
        val uriString = "content://media/external/video/media/1"
        val video = CapturedMedia.Video(uri = uriString, durationMs = 15000L)

        assertEquals(uriString, video.uri)
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
