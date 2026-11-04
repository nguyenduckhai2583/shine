package com.example.shine

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.shine.data.remote.TokenRefreshScheduler
import com.example.shine.data.remote.socket.SocketLifecycleManager
import com.example.shine.data.sync.ChannelSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShineApplication : Application(), CameraXConfig.Provider, Configuration.Provider {

    @Inject
    lateinit var tokenRefreshScheduler: TokenRefreshScheduler

    @Inject
    lateinit var socketLifecycleManager: SocketLifecycleManager

    @Inject
    lateinit var channelSyncScheduler: ChannelSyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        tokenRefreshScheduler.start()
        socketLifecycleManager.start()
        channelSyncScheduler.scheduleDaily7AmSync()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.Builder().build())
            .build()
    }
}
