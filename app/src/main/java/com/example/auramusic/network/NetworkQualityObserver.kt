package com.example.auramusic.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionQuality(val displayLabel: String, val shortBadge: String) {
    EXCELLENT_5G_WIFI("Lossless 5G/WiFi", "5G LOSSLESS"),
    CELLULAR_ROAMING("Roaming (Shield Active)", "ROAMING"),
    WEAK_VARIABLE_SIGNAL("Variable Signal (Adaptive)", "ADAPTIVE"),
    OFFLINE_SHIELD("Offline Shield (Playing Cache)", "OFFLINE")
}

/**
 * Auto Adaptive Audio Quality Tiers.
 * Dynamically assigned based on real-time signal strength & network bandwidth fluctuations.
 */
enum class AutoAudioQuality(
    val title: String,
    val shortBadge: String,
    val targetBitrateKbps: Int,
    val sampleRateInfo: String,
    val description: String
) {
    LOSSLESS_MASTER(
        "24-Bit FLAC (Lossless)",
        "AUTO 24-BIT",
        1411,
        "24-bit / 96kHz Master",
        "High bandwidth & strong signal. Streaming uncompressed studio master."
    ),
    HIGH_QUALITY_320(
        "Spotify High (320kbps)",
        "AUTO 320K",
        320,
        "320kbps Ultra-HD",
        "Good cellular/WiFi signal. High bitrate audio stream."
    ),
    BALANCED_256(
        "YouTube Music Opus (256kbps)",
        "AUTO 256K",
        256,
        "256kbps Opus HD",
        "Signal fluctuating. Dynamically balanced to eliminate buffering."
    ),
    DATA_SAVER_128(
        "Data Saver (128kbps)",
        "AUTO 128K",
        128,
        "128kbps Low Latency",
        "Weak signal or roaming detected. Adaptive buffer shield active."
    ),
    OFFLINE_CACHE(
        "Offline Shield",
        "OFFLINE",
        0,
        "Local 1GB Adaptive Cache",
        "Network connection lost. Seamlessly playing from local cache."
    )
}

data class NetworkStatus(
    val isConnected: Boolean = true,
    val isRoaming: Boolean = false,
    val isWifi: Boolean = true,
    val quality: ConnectionQuality = ConnectionQuality.EXCELLENT_5G_WIFI,
    val statusText: String = "5G Lossless Connected",
    val downstreamBandwidthKbps: Int = 15000,
    val signalPercent: Int = 95,
    val autoAudioQuality: AutoAudioQuality = AutoAudioQuality.LOSSLESS_MASTER,
    val isSignalFluctuating: Boolean = false,
    val fluctuationNote: String = "Signal strong & stable"
)

class NetworkQualityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var previousBandwidthKbps: Int = 15000

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
                statusText = "Offline • Smart Cache Active",
                downstreamBandwidthKbps = 0,
                signalPercent = 0,
                autoAudioQuality = AutoAudioQuality.OFFLINE_CACHE,
                isSignalFluctuating = false,
                fluctuationNote = "Offline Shield: Playing from 1GB cache"
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
            statusText = "Offline • Smart Cache Active",
            downstreamBandwidthKbps = 0,
            signalPercent = 0,
            autoAudioQuality = AutoAudioQuality.OFFLINE_CACHE,
            isSignalFluctuating = false,
            fluctuationNote = "Offline Shield: Playing from 1GB cache"
        )
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(
            isConnected = false,
            quality = ConnectionQuality.OFFLINE_SHIELD,
            statusText = "Offline • Smart Cache Active",
            downstreamBandwidthKbps = 0,
            signalPercent = 0,
            autoAudioQuality = AutoAudioQuality.OFFLINE_CACHE,
            isSignalFluctuating = false,
            fluctuationNote = "Offline Shield: Playing from 1GB cache"
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
                statusText = "Offline • Smart Cache Active",
                downstreamBandwidthKbps = 0,
                signalPercent = 0,
                autoAudioQuality = AutoAudioQuality.OFFLINE_CACHE,
                isSignalFluctuating = false,
                fluctuationNote = "Offline Shield: Playing from 1GB cache"
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

        val downstreamKbps = caps.linkDownstreamBandwidthKbps.coerceAtLeast(0)

        // Calculate Signal Strength Percentage (0 - 100%)
        val signalPercent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val signalDbm = caps.signalStrength
            if (signalDbm != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                // dBm usually ranges from -110 (very poor) to -50 (excellent)
                ((signalDbm - (-110)) * 100 / (-50 - (-110))).coerceIn(15, 100)
            } else {
                estimateSignalPercentFromBandwidth(downstreamKbps, isWifi)
            }
        } else {
            estimateSignalPercentFromBandwidth(downstreamKbps, isWifi)
        }

        // Detect Sudden Signal & Bandwidth Fluctuation
        val isFluctuating = previousBandwidthKbps > 4000 && downstreamKbps < 2500 ||
                (previousBandwidthKbps > 0 && downstreamKbps < (previousBandwidthKbps / 2))
        previousBandwidthKbps = downstreamKbps

        // Determine Adaptive Streaming Quality based on Signal & Bandwidth
        val autoQuality = when {
            !hasInternet -> AutoAudioQuality.OFFLINE_CACHE
            isRoaming -> AutoAudioQuality.DATA_SAVER_128
            downstreamKbps >= 6000 && signalPercent >= 70 -> AutoAudioQuality.LOSSLESS_MASTER
            downstreamKbps in 2500 until 6000 && signalPercent >= 50 -> AutoAudioQuality.HIGH_QUALITY_320
            downstreamKbps in 1000 until 2500 || isFluctuating -> AutoAudioQuality.BALANCED_256
            downstreamKbps < 1000 || signalPercent < 30 -> AutoAudioQuality.DATA_SAVER_128
            else -> AutoAudioQuality.LOSSLESS_MASTER
        }

        val connQuality = when (autoQuality) {
            AutoAudioQuality.LOSSLESS_MASTER -> ConnectionQuality.EXCELLENT_5G_WIFI
            AutoAudioQuality.HIGH_QUALITY_320 -> ConnectionQuality.EXCELLENT_5G_WIFI
            AutoAudioQuality.BALANCED_256 -> ConnectionQuality.WEAK_VARIABLE_SIGNAL
            AutoAudioQuality.DATA_SAVER_128 -> if (isRoaming) ConnectionQuality.CELLULAR_ROAMING else ConnectionQuality.WEAK_VARIABLE_SIGNAL
            AutoAudioQuality.OFFLINE_CACHE -> ConnectionQuality.OFFLINE_SHIELD
        }

        val fluctuationNote = when {
            !hasInternet -> "Offline Shield: Playing from 1GB cache"
            isRoaming -> "Roaming Detected: Switched to 128k Data Saver"
            isFluctuating -> "Signal Dip Detected: Auto-switched to 256k Opus buffer shield"
            autoQuality == AutoAudioQuality.LOSSLESS_MASTER -> if (isWifi) "WiFi Signal Strong: 24-bit FLAC Lossless active" else "5G Signal Strong: 24-bit FLAC Lossless active"
            autoQuality == AutoAudioQuality.HIGH_QUALITY_320 -> "4G Stable: 320kbps High Quality active"
            autoQuality == AutoAudioQuality.BALANCED_256 -> "Moderate Signal: 256kbps balanced stream active"
            else -> "Weak Signal: 128kbps low-latency stream active"
        }

        val text = when (autoQuality) {
            AutoAudioQuality.LOSSLESS_MASTER -> if (isWifi) "WiFi Lossless • 24-bit FLAC" else "5G Lossless • 24-bit FLAC"
            AutoAudioQuality.HIGH_QUALITY_320 -> "4G High Quality • 320kbps"
            AutoAudioQuality.BALANCED_256 -> "Adaptive Opus • 256kbps"
            AutoAudioQuality.DATA_SAVER_128 -> if (isRoaming) "Roaming • 128k Shield" else "Low Coverage • 128k Shield"
            AutoAudioQuality.OFFLINE_CACHE -> "Offline • Cache Shield Active"
        }

        return NetworkStatus(
            isConnected = hasInternet,
            isRoaming = isRoaming,
            isWifi = isWifi,
            quality = connQuality,
            statusText = text,
            downstreamBandwidthKbps = downstreamKbps,
            signalPercent = signalPercent,
            autoAudioQuality = autoQuality,
            isSignalFluctuating = isFluctuating,
            fluctuationNote = fluctuationNote
        )
    }

    private fun estimateSignalPercentFromBandwidth(downstreamKbps: Int, isWifi: Boolean): Int {
        return when {
            downstreamKbps > 12000 -> 98
            downstreamKbps > 6000 -> 85
            downstreamKbps > 3000 -> 70
            downstreamKbps > 1500 -> 50
            downstreamKbps > 500 -> 30
            else -> if (isWifi) 40 else 20
        }
    }
}
