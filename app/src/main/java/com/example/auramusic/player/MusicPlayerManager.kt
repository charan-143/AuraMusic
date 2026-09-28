package com.example.auramusic.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.example.auramusic.MainActivity
import com.example.auramusic.service.AuraMediaService
import com.example.auramusic.audio.EqualizerManager
import com.example.auramusic.cache.AdaptiveAudioCacheManager
import com.example.auramusic.model.AudioBitrateMode
import com.example.auramusic.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionTickerJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    val cacheManager by lazy { AdaptiveAudioCacheManager(context) }
    val equalizerManager by lazy { EqualizerManager(context) }

    val exoPlayer: ExoPlayer by lazy {
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheManager.cacheDataSourceFactory)
        val loadControl = cacheManager.createTravelLoadControl()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId > 0) {
                            equalizerManager.attachToSession(audioSessionId)
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startTicker()
                        } else {
                            stopTicker()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val duration = exoPlayer.duration.coerceAtLeast(0L)
                            val effectiveDuration = if (duration > 0) duration else (_playerState.value.currentTrack?.durationMs ?: 180000L)
                            _playerState.update {
                                it.copy(
                                    durationMs = effectiveDuration,
                                    bufferedPositionMs = exoPlayer.bufferedPosition
                                )
                            }
                            _playbackProgress.update {
                                it.copy(
                                    durationMs = effectiveDuration,
                                    bufferedPositionMs = exoPlayer.bufferedPosition
                                )
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            skipNext()
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        _playbackProgress.update {
                            it.copy(
                                currentPositionMs = newPosition.positionMs,
                                bufferedPositionMs = exoPlayer.bufferedPosition
                            )
                        }
                    }
                })
            }
    }

    val mediaSession: MediaSession by lazy {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        MediaSession.Builder(context, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            scope.launch { skipNext() }
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            scope.launch { skipPrevious() }
                            return SessionResult.RESULT_SUCCESS
                        }
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }
            })
            .build()
    }

    private fun startMediaPlaybackService() {
        try {
            val intent = Intent(context, AuraMediaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // safe fallback
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            detectAudioOutputDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            detectAudioOutputDevice()
        }
    }

    init {
        instance = this
        try {
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            // safe fallback
        }
        detectAudioOutputDevice()
        try {
            // Eagerly activate MediaSession for Android Quick Settings media controls
            val dummyToken = mediaSession.token
        } catch (e: Exception) {
            // safe fallback
        }

        scope.launch {
            equalizerManager.state.collect { eqState ->
                val badge = if (!eqState.isEnabled) {
                    "EQ OFF"
                } else if (eqState.isAiMode) {
                    "AI ✦ ${eqState.detectedProfileName.uppercase().take(12)}"
                } else {
                    "EQ ${eqState.currentPreset.displayName.uppercase()}"
                }
                _playerState.update {
                    it.copy(
                        isEqualizerEnabled = eqState.isEnabled,
                        equalizerProfileBadge = badge
                    )
                }
            }
        }
    }

    fun setQueue(tracks: List<Track>, initialIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val clampedIndex = initialIndex.coerceIn(0, tracks.lastIndex)
        val selected = tracks[clampedIndex]
        _playerState.update {
            it.copy(
                queue = tracks,
                currentIndex = clampedIndex,
                currentTrack = selected,
                durationMs = selected.durationMs,
                currentPositionMs = 0L,
                bufferedPositionMs = 0L
            )
        }
        _playbackProgress.value = PlaybackProgress(
            currentPositionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = selected.durationMs
        )
        // Predictive prefetch of next tracks
        cacheManager.prefetchUpcomingTracks(tracks, clampedIndex)
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val queue = newQueue ?: _playerState.value.queue.ifEmpty { listOf(track) }
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = index,
                currentPositionMs = 0L,
                bufferedPositionMs = 0L,
                durationMs = track.durationMs,
                isPlaying = true
            )
        }
        _playbackProgress.value = PlaybackProgress(
            currentPositionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = track.durationMs
        )

        // Trigger background preloading of upcoming tracks for seamless traveling
        cacheManager.prefetchUpcomingTracks(queue, index)

        equalizerManager.onTrackChanged(track)
        if (exoPlayer.audioSessionId > 0) {
            equalizerManager.attachToSession(exoPlayer.audioSessionId)
        }

        if (track.audioUrl.isNotBlank()) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(if (track.coverArtUrl.isNotBlank()) Uri.parse(track.coverArtUrl) else null)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(track.audioUrl))
                    .setMediaId(track.id)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                startMediaPlaybackService()
            } catch (e: Exception) {
                // Fallback to simulated playback ticker if stream error occurs
                startTicker()
                startMediaPlaybackService()
            }
        } else {
            // Simulated local playback progression with exact audio timings
            startTicker()
            startMediaPlaybackService()
        }
    }

    fun togglePlayPause() {
        val currentState = _playerState.value
        if (currentState.currentTrack == null && currentState.queue.isNotEmpty()) {
            playTrack(currentState.queue[0])
            return
        }

        if (currentState.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        _playerState.update { it.copy(isPlaying = false) }
        stopTicker()
    }

    fun resume() {
        val track = _playerState.value.currentTrack
        if (track != null && track.audioUrl.isNotBlank() && exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.play()
            startMediaPlaybackService()
        } else {
            _playerState.update { it.copy(isPlaying = true) }
            startTicker()
        }
    }

    fun seekToRatio(ratio: Float) {
        val currentDuration = _playbackProgress.value.durationMs.coerceAtLeast(_playerState.value.durationMs).coerceAtLeast(1000L)
        val targetMs = (ratio * currentDuration).toLong().coerceIn(0L, currentDuration)
        seekTo(targetMs)
    }

    fun seekTo(positionMs: Long) {
        if (exoPlayer.playbackState != Player.STATE_IDLE && _playerState.value.currentTrack?.audioUrl?.isNotBlank() == true) {
            exoPlayer.seekTo(positionMs)
        }
        _playbackProgress.update { it.copy(currentPositionMs = positionMs) }
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            state.queue.indices.random()
        } else {
            (state.currentIndex + 1) % state.queue.size
        }
        playTrack(state.queue[nextIndex])
    }

    fun skipPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        val currentMs = _playbackProgress.value.currentPositionMs
        if (currentMs > 3000L || state.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.lastIndex
        playTrack(state.queue[prevIndex])
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _playerState.update { it.copy(isRepeatOne = !it.isRepeatOne) }
    }

    fun toggleTravelMode() {
        _playerState.update { it.copy(isTravelModeEnabled = !it.isTravelModeEnabled) }
    }

    fun setBitrateMode(mode: AudioBitrateMode) {
        _playerState.update { it.copy(bitrateMode = mode) }
    }

    fun setAutoQualityEnabled(enabled: Boolean) {
        _playerState.update { it.copy(isAutoQualityEnabled = enabled) }
    }

    fun applyAdaptiveQuality(
        autoQuality: com.example.auramusic.network.AutoAudioQuality,
        bandwidthKbps: Int,
        signalPercent: Int,
        isFluctuating: Boolean = false,
        fluctuationNote: String = ""
    ) {
        if (!_playerState.value.isAutoQualityEnabled) return

        val bitrateMode = when (autoQuality) {
            com.example.auramusic.network.AutoAudioQuality.LOSSLESS_MASTER -> AudioBitrateMode.LOSSLESS_MASTER
            com.example.auramusic.network.AutoAudioQuality.HIGH_QUALITY_320 -> AudioBitrateMode.HIGH_QUALITY
            com.example.auramusic.network.AutoAudioQuality.BALANCED_256 -> AudioBitrateMode.BALANCED
            com.example.auramusic.network.AutoAudioQuality.DATA_SAVER_128 -> AudioBitrateMode.ADAPTIVE_ROAMING
            com.example.auramusic.network.AutoAudioQuality.OFFLINE_CACHE -> AudioBitrateMode.ADAPTIVE_ROAMING
        }

        // Dynamically configure ExoPlayer track selection bitrate constraint
        try {
            val maxBitrate = if (autoQuality.targetBitrateKbps > 0) autoQuality.targetBitrateKbps * 1000 else Int.MAX_VALUE
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxAudioBitrate(maxBitrate)
                .build()
        } catch (e: Exception) {
            // Safe fallback
        }

        // Dynamically update stream request headers
        cacheManager.updateStreamingQualityHeaders(autoQuality.targetBitrateKbps)
        equalizerManager.onStreamingQualityAdapted(bitrateMode.label)

        _playerState.update {
            it.copy(
                bitrateMode = bitrateMode,
                activeStreamingQualityBadge = autoQuality.shortBadge,
                activeStreamingQualityTitle = autoQuality.title,
                signalStrengthPercent = signalPercent,
                linkBandwidthKbps = bandwidthKbps,
                isSignalFluctuating = isFluctuating,
                autoQualitySwitchNote = if (fluctuationNote.isNotBlank()) fluctuationNote else autoQuality.description
            )
        }
    }

    fun setManualQuality(qualityTitle: String) {
        val (mode, badge, title, kbps) = when (qualityTitle) {
            "Spotify High (320kbps)" -> listOf(AudioBitrateMode.HIGH_QUALITY, "HQ 320K", "Spotify High (320kbps)", 320)
            "YouTube Music Opus (256kbps)" -> listOf(AudioBitrateMode.BALANCED, "OPUS 256K", "YouTube Music Opus (256kbps)", 256)
            "Data Saver (128kbps / Roaming)", "Data Saver (Roaming Auto)" -> listOf(AudioBitrateMode.ADAPTIVE_ROAMING, "SAVER 128K", "Data Saver (128kbps)", 128)
            else -> listOf(AudioBitrateMode.LOSSLESS_MASTER, "24-BIT FLAC", "Lossless Master (24-bit FLAC)", 1411)
        }

        val targetKbps = kbps as Int
        val targetMode = mode as AudioBitrateMode
        val targetBadge = badge as String
        val targetTitle = title as String

        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxAudioBitrate(targetKbps * 1000)
                .build()
        } catch (e: Exception) {
            // Safe fallback
        }

        cacheManager.updateStreamingQualityHeaders(targetKbps)

        _playerState.update {
            it.copy(
                isAutoQualityEnabled = false,
                bitrateMode = targetMode,
                activeStreamingQualityBadge = targetBadge,
                activeStreamingQualityTitle = targetTitle,
                autoQualitySwitchNote = "Manual Fixed: $targetTitle"
            )
        }
    }

    fun updateNetworkStatus(statusText: String) {
        _playerState.update { it.copy(networkStatusText = statusText) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun getAudioSessionId(): Int {
        return exoPlayer.audioSessionId
    }

    fun detectAudioOutputDevice() {
        try {
            val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()

            // 1. Check for Bluetooth Sink
            val btDevice = outputs.firstOrNull { dev ->
                dev.isSink && (
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                )
            }

            // 2. Check for Wired Sink
            val wiredDevice = outputs.firstOrNull { dev ->
                dev.isSink && (
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    dev.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                    dev.type == AudioDeviceInfo.TYPE_LINE_DIGITAL
                )
            }

            // 3. Check for USB Sink
            val usbDevice = outputs.firstOrNull { dev ->
                dev.isSink && (
                    dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                )
            }

            val (deviceName, deviceType) = when {
                btDevice != null -> {
                    val rawName = btDevice.productName?.toString()?.trim().orEmpty()
                    val name = if (rawName.isNotBlank() && !rawName.equals("Bluetooth", ignoreCase = true)) rawName else "Bluetooth Headphones"
                    name to "BLUETOOTH"
                }
                usbDevice != null -> {
                    val rawName = usbDevice.productName?.toString()?.trim().orEmpty()
                    val name = if (rawName.isNotBlank()) rawName else "USB-C Audio DAC"
                    name to "USB"
                }
                wiredDevice != null -> {
                    val rawName = wiredDevice.productName?.toString()?.trim().orEmpty()
                    val name = if (rawName.isNotBlank()) rawName else "Wired Headphones"
                    name to "WIRED"
                }
                else -> {
                    "Phone Speaker" to "SPEAKER"
                }
            }

            _playerState.update {
                it.copy(
                    activeAudioOutputDevice = deviceName,
                    activeAudioDeviceType = deviceType,
                    isAudioRouteAutoDetected = true
                )
            }
        } catch (e: Exception) {
            _playerState.update {
                it.copy(
                    activeAudioOutputDevice = "Phone Speaker",
                    activeAudioDeviceType = "SPEAKER",
                    isAudioRouteAutoDetected = true
                )
            }
        }
    }

    fun resetAudioOutputDeviceToAuto() {
        detectAudioOutputDevice()
    }

    fun setActiveAudioOutputDevice(name: String) {
        if (name.contains("Auto", ignoreCase = true)) {
            detectAudioOutputDevice()
            return
        }
        val type = when {
            name.contains("Bluetooth", ignoreCase = true) || name.contains("Buds", ignoreCase = true) || name.contains("WH-", ignoreCase = true) || name.contains("AirPods", ignoreCase = true) -> "BLUETOOTH"
            name.contains("USB", ignoreCase = true) || name.contains("DAC", ignoreCase = true) -> "USB"
            name.contains("Wired", ignoreCase = true) || name.contains("Headphone", ignoreCase = true) -> "WIRED"
            name.contains("Speaker", ignoreCase = true) -> "SPEAKER"
            else -> "BLUETOOTH"
        }
        _playerState.update {
            it.copy(
                activeAudioOutputDevice = name,
                activeAudioDeviceType = type,
                isAudioRouteAutoDetected = false
            )
        }
    }

    fun clearQueue() {
        val current = _playerState.value.currentTrack
        _playerState.update {
            it.copy(
                queue = if (current != null) listOf(current) else emptyList(),
                currentIndex = 0
            )
        }
    }

    fun addToQueue(track: Track) {
        val currentQueue = _playerState.value.queue
        if (currentQueue.any { it.id == track.id }) return
        _playerState.update { it.copy(queue = currentQueue + track) }
    }

    fun removeFromQueue(trackId: String) {
        val state = _playerState.value
        val newQueue = state.queue.filter { it.id != trackId }
        if (newQueue.isEmpty()) {
            pause()
            _playerState.update {
                it.copy(
                    queue = emptyList(),
                    currentTrack = null,
                    currentIndex = 0,
                    isPlaying = false,
                    currentPositionMs = 0L
                )
            }
            return
        }
        val isRemovingCurrent = state.currentTrack?.id == trackId
        val newIndex = if (isRemovingCurrent) {
            state.currentIndex.coerceAtMost(newQueue.lastIndex)
        } else {
            newQueue.indexOfFirst { it.id == state.currentTrack?.id }.coerceAtLeast(0)
        }
        val nextTrack = if (isRemovingCurrent) newQueue[newIndex] else state.currentTrack
        _playerState.update {
            it.copy(
                queue = newQueue,
                currentIndex = newIndex,
                currentTrack = nextTrack
            )
        }
        if (isRemovingCurrent && nextTrack != null) {
            playTrack(nextTrack, newQueue)
        }
    }

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.update { it.copy(sleepTimerRemainingSec = null) }
            return
        }
        var remainingSec = minutes * 60
        _playerState.update { it.copy(sleepTimerRemainingSec = remainingSec) }
        sleepTimerJob = scope.launch {
            while (remainingSec > 0 && isActive) {
                delay(1000)
                remainingSec--
                _playerState.update { it.copy(sleepTimerRemainingSec = remainingSec) }
            }
            if (remainingSec <= 0) {
                pause()
                _playerState.update { it.copy(sleepTimerRemainingSec = null) }
            }
        }
    }

    private fun startTicker() {
        stopTicker()
        positionTickerJob = scope.launch {
            while (isActive) {
                delay(250)
                val state = _playerState.value
                if (state.isPlaying) {
                    if (exoPlayer.isPlaying) {
                        val current = exoPlayer.currentPosition
                        val duration = exoPlayer.duration.coerceAtLeast(state.durationMs)
                        val buffered = exoPlayer.bufferedPosition
                        _playbackProgress.value = PlaybackProgress(
                            currentPositionMs = current,
                            durationMs = duration,
                            bufferedPositionMs = buffered
                        )
                    } else {
                        // Simulated progression
                        val currentProg = _playbackProgress.value
                        val speedFactor = state.playbackSpeed
                        val newPos = (currentProg.currentPositionMs + (250 * speedFactor).toLong())
                        val simDuration = state.durationMs.coerceAtLeast(currentProg.durationMs)
                        val simulatedBuffer = (newPos + 60000L).coerceAtMost(simDuration)
                        if (newPos >= simDuration && simDuration > 0) {
                            if (state.isRepeatOne) {
                                _playbackProgress.value = PlaybackProgress(currentPositionMs = 0L, durationMs = simDuration)
                            } else {
                                skipNext()
                            }
                        } else {
                            _playbackProgress.value = PlaybackProgress(
                                currentPositionMs = newPos,
                                bufferedPositionMs = simulatedBuffer,
                                durationMs = simDuration
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopTicker()
        try {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            // safe fallback
        }
        try {
            mediaSession.release()
        } catch (e: Exception) {
            // safe fallback
        }
        equalizerManager.release()
        exoPlayer.release()
    }
}
