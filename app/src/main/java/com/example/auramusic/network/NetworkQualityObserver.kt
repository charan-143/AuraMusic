package com.example.auramusic.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionQuality(val displayLabel: String, val shortBadge: String) {
    EXCELLENT_5G_WIFI("Lossless 5G/WiFi", "5G LOSSLESS"),
    CELLULAR_ROAMING("Roaming (Shield Active)", "ROAMING"),
    WEAK_VARIABLE_SIGNAL("Variable Signal (Adaptive)", "ADAPTIVE"),
    OFFLINE_SHIELD("Offline Shield (Playing Cache)", "OFFLINE")
}

data class NetworkStatus(
    val isConnected: Boolean = true,
    val isRoaming: Boolean = false,
    val isWifi: Boolean = true,
    val quality: ConnectionQuality = ConnectionQuality.EXCELLENT_5G_WIFI,
    val statusText: String = "5G Lossless Connected"
)

class NetworkQualityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(getCurrentStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            _networkStatus.value = NetworkStatus(
                isConnected = false,
                isRoaming = false,
                isWifi = false,
                quality = ConnectionQuality.OFFLINE_SHIELD,
                statusText = "Offline • Smart Cache Active"
            )
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            evaluateCapabilities(capabilities)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        updateNetworkStatus()
    }

    private fun getCurrentStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkStatus(
            isConnected = false,
            quality = ConnectionQuality.OFFLINE_SHIELD,
            statusText = "Offline • Smart Cache Active"
        )
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(
            isConnected = false,
            quality = ConnectionQuality.OFFLINE_SHIELD,
            statusText = "Offline • Smart Cache Active"
        )
        return evaluateCaps(caps)
    }

    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        if (caps != null) {
            evaluateCapabilities(caps)
        } else {
            _networkStatus.value = NetworkStatus(
                isConnected = false,
                quality = ConnectionQuality.OFFLINE_SHIELD,
                statusText = "Offline • Smart Cache Active"
            )
        }
    }

    private fun evaluateCapabilities(caps: NetworkCapabilities) {
        _networkStatus.value = evaluateCaps(caps)
    }

    private fun evaluateCaps(caps: NetworkCapabilities): NetworkStatus {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isNotRoaming = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        val isRoaming = !isNotRoaming
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val downstreamKbps = caps.linkDownstreamBandwidthKbps

        val quality = when {
            !hasInternet -> ConnectionQuality.OFFLINE_SHIELD
            isRoaming -> ConnectionQuality.CELLULAR_ROAMING
            isWifi || (isCellular && downstreamKbps > 5000) -> ConnectionQuality.EXCELLENT_5G_WIFI
            downstreamKbps in 1..2500 -> ConnectionQuality.WEAK_VARIABLE_SIGNAL
            else -> ConnectionQuality.EXCELLENT_5G_WIFI
        }

        val text = when (quality) {
            ConnectionQuality.EXCELLENT_5G_WIFI -> if (isWifi) "WiFi Lossless • 1411kbps" else "5G Lossless • 320kbps"
            ConnectionQuality.CELLULAR_ROAMING -> "Roaming • Smart Pre-buffer Active"
            ConnectionQuality.WEAK_VARIABLE_SIGNAL -> "Low Coverage • ABR Active"
            ConnectionQuality.OFFLINE_SHIELD -> "Offline • Cache Shield Active"
        }

        return NetworkStatus(
            isConnected = hasInternet,
            isRoaming = isRoaming,
            isWifi = isWifi,
            quality = quality,
            statusText = text
        )
    }
}
