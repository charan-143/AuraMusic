package com.example.auramusic.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.auramusic.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EqualizerManager(private val context: Context) {

    companion object {
        private const val TAG = "EqualizerManager"
        private const val PREFS_NAME = "aura_equalizer_settings"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_AI_MODE = "eq_ai_mode"
        private const val KEY_AI_TARGET = "eq_ai_target"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BASS = "eq_bass_boost"
        private const val KEY_VIRTUALIZER = "eq_virtualizer"
        private const val KEY_BAND_PREFIX = "eq_band_"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var hardwareEqualizer: Equalizer? = null
    private var hardwareBassBoost: BassBoost? = null
    private var hardwareVirtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var activeTrack: Track? = null
    private var activeBitrateModeName: String = "Auto (Signal Adaptive)"

    private fun loadInitialState(): EqualizerState {
        val isEnabled = prefs.getBoolean(KEY_ENABLED, true)
        val isAiMode = prefs.getBoolean(KEY_AI_MODE, true)
        val aiTargetName = prefs.getString(KEY_AI_TARGET, AiAudioTarget.AUTO_DETECT.name) ?: AiAudioTarget.AUTO_DETECT.name
        val aiTarget = try { AiAudioTarget.valueOf(aiTargetName) } catch (e: Exception) { AiAudioTarget.AUTO_DETECT }
        val presetName = prefs.getString(KEY_PRESET, EqualizerPreset.FLAT.name) ?: EqualizerPreset.FLAT.name
        val preset = try { EqualizerPreset.valueOf(presetName) } catch (e: Exception) { EqualizerPreset.FLAT }
        val bassBoost = prefs.getInt(KEY_BASS, 0)
        val virtualizer = prefs.getInt(KEY_VIRTUALIZER, 0)

        val defaultBands = listOf(
            EqualizerBand(0, 60, "60Hz", prefs.getInt("${KEY_BAND_PREFIX}0", 0)),
            EqualizerBand(1, 230, "230Hz", prefs.getInt("${KEY_BAND_PREFIX}1", 0)),
            EqualizerBand(2, 910, "910Hz", prefs.getInt("${KEY_BAND_PREFIX}2", 0)),
            EqualizerBand(3, 3600, "3.6kHz", prefs.getInt("${KEY_BAND_PREFIX}3", 0)),
            EqualizerBand(4, 14000, "14kHz", prefs.getInt("${KEY_BAND_PREFIX}4", 0))
        )

        return EqualizerState(
            isEnabled = isEnabled,
            isAiMode = isAiMode,
            activeAiTarget = aiTarget,
            currentPreset = preset,
            bands = defaultBands,
            bassBoostStrength = bassBoost,
            virtualizerStrength = virtualizer,
            isHardwareAttached = false
        )
    }

    /**
     * Attaches hardware audio effects to ExoPlayer's audioSessionId safely.
     */
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAudioSessionId == audioSessionId && hardwareEqualizer != null) return

        releaseHardware()
        currentAudioSessionId = audioSessionId

        try {
            // Android Equalizer (priority 0)
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = _state.value.isEnabled

            // Sync bands if available
            val numBands = eq.numberOfBands.toInt()
            val dynamicBands = if (numBands >= 5) {
                (0 until numBands.coerceAtMost(5)).map { bandIdx ->
                    val centerMhz = eq.getCenterFreq(bandIdx.toShort())
                    val hz = centerMhz / 1000
                    val label = if (hz >= 1000) "${String.format("%.1f", hz / 1000f).replace(".0", "")}kHz" else "${hz}Hz"
                    val currentMilliBels = _state.value.bands.getOrNull(bandIdx)?.levelMilliBels ?: 0
                    try {
                        eq.setBandLevel(bandIdx.toShort(), currentMilliBels.coerceIn(-1200, 1200).toShort())
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set initial band $bandIdx", e)
                    }
                    EqualizerBand(bandIdx, hz, label, currentMilliBels)
                }
            } else {
                _state.value.bands
            }

            hardwareEqualizer = eq

            // Hardware Bass Boost
            try {
                val bb = BassBoost(0, audioSessionId)
                if (bb.strengthSupported) {
                    bb.setStrength(_state.value.bassBoostStrength.toShort())
                    bb.enabled = _state.value.isEnabled && _state.value.bassBoostStrength > 0
                    hardwareBassBoost = bb
                }
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost not supported on this session", e)
            }

            // Hardware Virtualizer (Spatial)
            try {
                val virt = Virtualizer(0, audioSessionId)
                if (virt.strengthSupported) {
                    virt.setStrength(_state.value.virtualizerStrength.toShort())
                    virt.enabled = _state.value.isEnabled && _state.value.virtualizerStrength > 0
                    hardwareVirtualizer = virt
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer not supported on this session", e)
            }

            _state.update {
                it.copy(
                    bands = dynamicBands,
                    isHardwareAttached = true
                )
            }
            Log.d(TAG, "Equalizer successfully attached to session $audioSessionId with ${eq.numberOfBands} bands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach hardware equalizer to session $audioSessionId: ${e.message}")
            _state.update { it.copy(isHardwareAttached = false) }
        }

        // Run AI evaluation on current track if in AI mode
        if (_state.value.isAiMode) {
            recalculateAiEqualizer()
        }
    }

    /**
     * Toggles master bypass switch.
     */
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.update { it.copy(isEnabled = enabled) }

        try {
            hardwareEqualizer?.enabled = enabled
            hardwareBassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
            hardwareVirtualizer?.enabled = enabled && _state.value.virtualizerStrength > 0
        } catch (e: Exception) {
            Log.w(TAG, "Error updating hardware effect enabled state", e)
        }
    }

    /**
     * Switches between AI Assisted mode and Manual Graphic mode.
     */
    fun setAiMode(isAiMode: Boolean) {
        prefs.edit().putBoolean(KEY_AI_MODE, isAiMode).apply()
        _state.update { it.copy(isAiMode = isAiMode) }

        if (isAiMode) {
            recalculateAiEqualizer()
        } else {
            // Restore manual preset or custom bands
            applyBandsToHardware(_state.value.bands)
            applyBassBoostToHardware(_state.value.bassBoostStrength)
            applyVirtualizerToHardware(_state.value.virtualizerStrength)
        }
    }

    /**
     * Sets user's acoustic target profile in AI mode.
     */
    fun setAiTarget(target: AiAudioTarget) {
        prefs.edit().putString(KEY_AI_TARGET, target.name).apply()
        _state.update { it.copy(activeAiTarget = target) }
        if (_state.value.isAiMode) {
            recalculateAiEqualizer()
        }
    }

    /**
     * Manual adjustment of a single band in millibels (-1200 to +1200).
     */
    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val clamped = levelMilliBels.coerceIn(-1200, 1200)
        prefs.edit().putInt("${KEY_BAND_PREFIX}$bandIndex", clamped).apply()
        prefs.edit().putString(KEY_PRESET, EqualizerPreset.CUSTOM.name).apply()

        _state.update { current ->
            val updatedBands = current.bands.map { band ->
                if (band.index == bandIndex) band.copy(levelMilliBels = clamped) else band
            }
            current.copy(
                bands = updatedBands,
                currentPreset = EqualizerPreset.CUSTOM,
                isAiMode = false
            )
        }
        prefs.edit().putBoolean(KEY_AI_MODE, false).apply()

        try {
            hardwareEqualizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "Error setting band $bandIndex to $clamped", e)
        }
    }

    /**
     * Sets Bass Boost strength (0 to 1000).
     */
    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        prefs.edit().putInt(KEY_BASS, clamped).apply()
        _state.update { it.copy(bassBoostStrength = clamped) }
        applyBassBoostToHardware(clamped)
    }

    /**
     * Sets Virtualizer 3D spatial widening strength (0 to 1000).
     */
    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        prefs.edit().putInt(KEY_VIRTUALIZER, clamped).apply()
        _state.update { it.copy(virtualizerStrength = clamped) }
        applyVirtualizerToHardware(clamped)
    }

    /**
     * Applies a curated preset.
     */
    fun applyPreset(preset: EqualizerPreset) {
        prefs.edit().putString(KEY_PRESET, preset.name).apply()
        prefs.edit().putBoolean(KEY_AI_MODE, false).apply()

        val (bandLevels, bass, virt) = getPresetGains(preset)

        _state.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val level = bandLevels.getOrElse(idx) { 0 }
                prefs.edit().putInt("${KEY_BAND_PREFIX}$idx", level).apply()
                band.copy(levelMilliBels = level)
            }
            current.copy(
                currentPreset = preset,
                isAiMode = false,
                bands = updatedBands,
                bassBoostStrength = bass,
                virtualizerStrength = virt
            )
        }

        applyBandsToHardware(_state.value.bands)
        applyBassBoostToHardware(bass)
        applyVirtualizerToHardware(virt)
    }

    /**
     * Resets all bands and effects to Flat reference.
     */
    fun resetToFlat() {
        applyPreset(EqualizerPreset.FLAT)
    }

    /**
     * Called when active track changes.
     */
    fun onTrackChanged(track: Track?) {
        activeTrack = track
        if (_state.value.isAiMode) {
            recalculateAiEqualizer()
        }
    }

    /**
     * Called when adaptive quality changes due to signal fluctuation.
     */
    fun onStreamingQualityAdapted(bitrateModeName: String) {
        activeBitrateModeName = bitrateModeName
        if (_state.value.isAiMode) {
            recalculateAiEqualizer()
        }
    }

    private fun recalculateAiEqualizer() {
        val track = activeTrack
        val target = _state.value.activeAiTarget
        val isLossless = track?.isLossless == true
        val isDataSaver = activeBitrateModeName.contains("Data Saver", ignoreCase = true) ||
                activeBitrateModeName.contains("128", ignoreCase = true)

        val analysis = AiEqualizerEngine.analyze(
            track = track,
            target = target,
            isLossless = isLossless,
            isDataSaver = isDataSaver
        )

        _state.update { current ->
            val newBands = current.bands.mapIndexed { idx, band ->
                band.copy(levelMilliBels = analysis.bandLevels.getOrElse(idx) { 0 })
            }
            current.copy(
                detectedProfileName = analysis.profileName,
                aiRationaleTitle = analysis.rationaleTitle,
                aiRationaleDescription = analysis.rationaleDescription,
                bands = newBands,
                bassBoostStrength = analysis.bassBoost,
                virtualizerStrength = analysis.virtualizer
            )
        }

        applyBandsToHardware(_state.value.bands)
        applyBassBoostToHardware(analysis.bassBoost)
        applyVirtualizerToHardware(analysis.virtualizer)
    }

    private fun applyBandsToHardware(bands: List<EqualizerBand>) {
        val eq = hardwareEqualizer ?: return
        try {
            bands.forEach { band ->
                if (band.index < eq.numberOfBands) {
                    eq.setBandLevel(band.index.toShort(), band.levelMilliBels.coerceIn(-1200, 1200).toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying bands to hardware", e)
        }
    }

    private fun applyBassBoostToHardware(strength: Int) {
        val bb = hardwareBassBoost ?: return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
                bb.enabled = _state.value.isEnabled && strength > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting hardware bass boost", e)
        }
    }

    private fun applyVirtualizerToHardware(strength: Int) {
        val virt = hardwareVirtualizer ?: return
        try {
            if (virt.strengthSupported) {
                virt.setStrength(strength.coerceIn(0, 1000).toShort())
                virt.enabled = _state.value.isEnabled && strength > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting hardware virtualizer", e)
        }
    }

    private fun getPresetGains(preset: EqualizerPreset): Triple<List<Int>, Int, Int> {
        return when (preset) {
            EqualizerPreset.FLAT -> Triple(listOf(0, 0, 0, 0, 0), 0, 0)
            EqualizerPreset.BASS_HEAVY -> Triple(listOf(600, 400, -100, 100, 200), 700, 200)
            EqualizerPreset.VOCAL_LIFT -> Triple(listOf(-200, 100, 400, 500, 200), 0, 300)
            EqualizerPreset.ROCK -> Triple(listOf(450, 250, -200, 350, 500), 400, 350)
            EqualizerPreset.POP -> Triple(listOf(200, 100, 250, 350, 400), 300, 250)
            EqualizerPreset.ELECTRONIC -> Triple(listOf(650, 300, -250, 200, 500), 650, 450)
            EqualizerPreset.CLASSICAL -> Triple(listOf(300, 200, 0, 250, 450), 100, 600)
            EqualizerPreset.HIP_HOP -> Triple(listOf(700, 400, 0, 200, 300), 750, 200)
            EqualizerPreset.ACOUSTIC -> Triple(listOf(200, 300, 200, 300, 400), 150, 400)
            EqualizerPreset.CUSTOM -> Triple(_state.value.bands.map { it.levelMilliBels }, _state.value.bassBoostStrength, _state.value.virtualizerStrength)
        }
    }

    fun release() {
        releaseHardware()
    }

    private fun releaseHardware() {
        try {
            hardwareEqualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Equalizer", e)
        }
        hardwareEqualizer = null

        try {
            hardwareBassBoost?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing BassBoost", e)
        }
        hardwareBassBoost = null

        try {
            hardwareVirtualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Virtualizer", e)
        }
        hardwareVirtualizer = null
    }
}

/**
 * On-device heuristic psychoacoustic AI analysis engine.
 */
object AiEqualizerEngine {

    data class AiEqualizerResult(
        val profileName: String,
        val rationaleTitle: String,
        val rationaleDescription: String,
        val bandLevels: List<Int>, // 5 bands in milliBels
        val bassBoost: Int,
        val virtualizer: Int
    )

    fun analyze(
        track: Track?,
        target: AiAudioTarget,
        isLossless: Boolean,
        isDataSaver: Boolean
    ): AiEqualizerResult {
        val title = track?.title ?: "No Track"
        val artist = track?.artist ?: "Unknown Artist"
        val textToMatch = "$title $artist ${track?.album ?: ""}".lowercase()

        // 1. If user set a specific target profile (not AUTO_DETECT), prioritize it
        if (target != AiAudioTarget.AUTO_DETECT) {
            return when (target) {
                AiAudioTarget.CINEMATIC_SOUNDSTAGE -> AiEqualizerResult(
                    profileName = "Cinematic Soundstage",
                    rationaleTitle = "Dynamic Symphonic Soundstage",
                    rationaleDescription = "Elevated deep sub-bass (60Hz +4.0dB) and expanded spatial 3D stereo width (75%) for immersive, epic cinematic dynamics.",
                    bandLevels = listOf(400, 200, 0, 300, if (isLossless) 550 else 350),
                    bassBoost = 350,
                    virtualizer = 750
                )
                AiAudioTarget.VOCAL_PRESENCE -> AiEqualizerResult(
                    profileName = "Vocal Presence",
                    rationaleTitle = "Intelligibility & Intimacy Lift",
                    rationaleDescription = "Attenuated low-end rumble with a targeted presence boost at 910Hz (+3.5dB) and 3.6kHz (+4.5dB) to bring lead vocals forward.",
                    bandLevels = listOf(-150, 100, 350, 450, 300),
                    bassBoost = 0,
                    virtualizer = 300
                )
                AiAudioTarget.SUB_BASS_PUNCH -> AiEqualizerResult(
                    profileName = "Sub-Bass Punch",
                    rationaleTitle = "OLED Low-Frequency Impact",
                    rationaleDescription = "Heavy sub-bass shelf at 60Hz (+6.5dB) coupled with hardware Bass Boost (70%) and scooped lower-mids to eliminate muddiness.",
                    bandLevels = listOf(650, 350, -200, 250, 450),
                    bassBoost = 700,
                    virtualizer = 350
                )
                AiAudioTarget.WARM_VINTAGE -> AiEqualizerResult(
                    profileName = "Warm Analog Tape",
                    rationaleTitle = "Vintage Tube Warmth",
                    rationaleDescription = "Gently rolled-off high frequencies (-2.0dB at 14kHz) with enriched 230Hz (+3.5dB) body replicating warm magnetic tape saturation.",
                    bandLevels = listOf(200, 350, 200, 100, -200),
                    bassBoost = 200,
                    virtualizer = 400
                )
                AiAudioTarget.STUDIO_FLAT -> AiEqualizerResult(
                    profileName = "Studio Reference Pure",
                    rationaleTitle = "Linear Bit-Perfect Monitoring",
                    rationaleDescription = "Flat uncolored frequency response with zero phase distortion, providing pure transparent reproduction of the studio recording.",
                    bandLevels = listOf(0, 0, 0, 0, 0),
                    bassBoost = 0,
                    virtualizer = 0
                )
                AiAudioTarget.AUTO_DETECT -> error("Handled below")
            }
        }

        // 2. AUTO_DETECT heuristics based on track metadata
        val isSoundtrack = textToMatch.contains("soundtrack") || textToMatch.contains("hans zimmer") ||
                textToMatch.contains("interstellar") || textToMatch.contains("inception") ||
                textToMatch.contains("dune") || textToMatch.contains("john williams") ||
                textToMatch.contains("ludwig") || textToMatch.contains("orchestra") ||
                textToMatch.contains("symphony") || textToMatch.contains("film") ||
                textToMatch.contains("score") || textToMatch.contains("richter")

        val isElectronic = textToMatch.contains("daft punk") || textToMatch.contains("skrillex") ||
                textToMatch.contains("marshmello") || textToMatch.contains("remix") ||
                textToMatch.contains("edm") || textToMatch.contains("house") ||
                textToMatch.contains("techno") || textToMatch.contains("trap") ||
                textToMatch.contains("dubstep") || textToMatch.contains("avicii") ||
                textToMatch.contains("electronic") || textToMatch.contains("synth")

        val isVocalPop = textToMatch.contains("adele") || textToMatch.contains("taylor") ||
                textToMatch.contains("ed sheeran") || textToMatch.contains("billie eilish") ||
                textToMatch.contains("pop") || textToMatch.contains("vocal") ||
                textToMatch.contains("acoustic") || textToMatch.contains("piano") ||
                textToMatch.contains("unplugged") || textToMatch.contains("olivia")

        val isRockMetal = textToMatch.contains("queen") || textToMatch.contains("metallica") ||
                textToMatch.contains("rock") || textToMatch.contains("metal") ||
                textToMatch.contains("ac/dc") || textToMatch.contains("nirvana") ||
                textToMatch.contains("linkin park") || textToMatch.contains("guitar")

        val isHipHop = textToMatch.contains("drake") || textToMatch.contains("kendrick") ||
                textToMatch.contains("eminem") || textToMatch.contains("hip hop") ||
                textToMatch.contains("rap") || textToMatch.contains("beat") ||
                textToMatch.contains("travis") || textToMatch.contains("kanye")

        return when {
            isSoundtrack -> AiEqualizerResult(
                profileName = "Cinematic Soundstage",
                rationaleTitle = "AI Detected: Symphonic Soundtrack",
                rationaleDescription = "Tuned for grand cinematic scope: boosted deep sub-bass (+4.0dB at 60Hz) for rumbling brass and expanded 3D spatial virtualizer (75%) for sweeping orchestral separation.",
                bandLevels = listOf(400, 200, 0, 300, if (isLossless) 550 else 350),
                bassBoost = 350,
                virtualizer = 750
            )

            isElectronic -> AiEqualizerResult(
                profileName = "Sub-Bass & Sparkle",
                rationaleTitle = "AI Detected: Electronic / EDM",
                rationaleDescription = "Sculpted for electronic energy: high-impact sub-bass (+6.5dB at 60Hz) with tightened mid-bass and scooped 910Hz to prevent mix mud.",
                bandLevels = listOf(650, 350, -200, 250, if (isDataSaver) 200 else 500),
                bassBoost = 650,
                virtualizer = 400
            )

            isVocalPop -> AiEqualizerResult(
                profileName = "Vocal Clarity Lift",
                rationaleTitle = "AI Detected: Vocal & Acoustic",
                rationaleDescription = "Centered on human voice intelligibility: high-pass rumble filter at 60Hz (-1.5dB) paired with sweet presence lift at 3.6kHz (+4.5dB).",
                bandLevels = listOf(-150, 100, 350, 450, 300),
                bassBoost = 0,
                virtualizer = 300
            )

            isRockMetal -> AiEqualizerResult(
                profileName = "Dynamic Contour V",
                rationaleTitle = "AI Detected: Rock & Alternative",
                rationaleDescription = "High-energy V-contour: punchy kick drum and bass guitar (+4.5dB at 60Hz) with aggressive upper midrange sizzle (+5.0dB at 14kHz) for guitar bite.",
                bandLevels = listOf(450, 250, -200, 350, if (isDataSaver) 250 else 500),
                bassBoost = 400,
                virtualizer = 350
            )

            isHipHop -> AiEqualizerResult(
                profileName = "Deep 808 Sub-Bass",
                rationaleTitle = "AI Detected: Hip-Hop & Urban",
                rationaleDescription = "Tuned for 808 kick dominance (+7.0dB at 60Hz) with 75% Bass Boost and clear vocal clarity at 3.6kHz.",
                bandLevels = listOf(700, 400, 0, 250, 300),
                bassBoost = 750,
                virtualizer = 200
            )

            else -> {
                // Adaptive default based on stream fidelity
                if (isLossless) {
                    AiEqualizerResult(
                        profileName = "Lossless Master Dynamics",
                        rationaleTitle = "AI Detected: High-Resolution 24-Bit FLAC",
                        rationaleDescription = "Optimized for full-bandwidth lossless fidelity: linear neutral lower-mids with subtle ultra-high frequency air (+4.0dB at 14kHz) for micro-transient clarity.",
                        bandLevels = listOf(200, 100, 0, 200, 400),
                        bassBoost = 150,
                        virtualizer = 450
                    )
                } else if (isDataSaver) {
                    AiEqualizerResult(
                        profileName = "Adaptive Compression Shield",
                        rationaleTitle = "AI Detected: Signal Dip / Data Saver Stream",
                        rationaleDescription = "Compensating for mobile signal fluctuations: slightly rolled-off 14kHz to soften compression artifacts, paired with warm 230Hz (+3.0dB) fullness.",
                        bandLevels = listOf(250, 300, 100, 100, -100),
                        bassBoost = 200,
                        virtualizer = 250
                    )
                } else {
                    AiEqualizerResult(
                        profileName = "Balanced Dynamic Profile",
                        rationaleTitle = "AI Auto-Tuned Stream",
                        rationaleDescription = "Balanced acoustic tuning providing natural stereo depth, gentle bass extension (+3.0dB), and transparent midrange presence.",
                        bandLevels = listOf(300, 150, 0, 200, 350),
                        bassBoost = 250,
                        virtualizer = 350
                    )
                }
            }
        }
    }
}
