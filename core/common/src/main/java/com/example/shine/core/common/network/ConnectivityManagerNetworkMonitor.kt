package com.example.shine.core.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    private val _isOnline = MutableStateFlow(isCurrentlyConnected())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                _isOnline.update { networkCapabilities.isValidatedInternet() }
            }

            override fun onLost(network: Network) {
                _isOnline.update { isCurrentlyConnected() }
            }
        }

        try {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register default network callback", e)
        }
    }

    private fun isCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(network).isValidatedInternet()
    }

    private fun NetworkCapabilities?.isValidatedInternet(): Boolean {
        return this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        private const val TAG = "ConnectivityManagerNetworkMonitor"
    }
}
