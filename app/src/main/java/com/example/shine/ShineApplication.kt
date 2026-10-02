package com.example.shine

import android.app.Application
import com.example.shine.data.remote.TokenRefreshScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShineApplication : Application() {

    @Inject
    lateinit var tokenRefreshScheduler: TokenRefreshScheduler

    override fun onCreate() {
        super.onCreate()
        tokenRefreshScheduler.start()
    }
}
