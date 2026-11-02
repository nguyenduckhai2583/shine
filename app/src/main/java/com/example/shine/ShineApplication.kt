package com.example.shine

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import com.example.shine.data.remote.TokenRefreshScheduler
import com.example.shine.data.remote.socket.SocketLifecycleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShineApplication : Application(), CameraXConfig.Provider {

    @Inject
    lateinit var tokenRefreshScheduler: TokenRefreshScheduler

    @Inject
    lateinit var socketLifecycleManager: SocketLifecycleManager

    override fun onCreate() {
        super.onCreate()
        tokenRefreshScheduler.start()
        socketLifecycleManager.start()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.Builder().build())
            .build()
    }
}
