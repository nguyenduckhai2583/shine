package com.example.shine.core.common.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPermissionTest {

    @Test
    fun camera_containsCameraPermission() {
        val permissions = AppPermission.Camera.manifestPermissions
        assertEquals(1, permissions.size)
        assertEquals(Manifest.permission.CAMERA, permissions.first())
    }

    @Test
    fun recordAudio_containsAudioPermission() {
        val permissions = AppPermission.RecordAudio.manifestPermissions
        assertEquals(1, permissions.size)
        assertEquals(Manifest.permission.RECORD_AUDIO, permissions.first())
    }

    @Test
    fun video_containsBothCameraAndAudioPermissions() {
        val permissions = AppPermission.Video.manifestPermissions
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.CAMERA))
        assertTrue(permissions.contains(Manifest.permission.RECORD_AUDIO))
    }
}
