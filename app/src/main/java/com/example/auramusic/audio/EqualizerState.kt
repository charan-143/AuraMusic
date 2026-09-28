package com.example.auramusic.audio

enum class EqualizerPreset(val displayName: String) {
    FLAT("Flat"),
    BASS_HEAVY("Bass Heavy"),
    VOCAL_LIFT("Vocal Lift"),
    ROCK("Rock"),
    POP("Pop"),
    ELECTRONIC("Electronic"),
    CLASSICAL("Classical"),
    HIP_HOP("Hip-Hop"),
    ACOUSTIC("Acoustic"),
    CUSTOM("Custom")
}

enum class AiAudioTarget(val displayName: String, val tag: String) {
    AUTO_DETECT("Auto-Detect (Adaptive)", "AI AUTO"),
    CINEMATIC_SOUNDSTAGE("Cinematic Soundstage", "CINEMATIC"),
    VOCAL_PRESENCE("Vocal Presence", "VOCALS"),
    SUB_BASS_PUNCH("Sub-Bass Punch", "BASS"),
    WARM_VINTAGE("Warm Analog Tape", "WARMTH"),
    STUDIO_FLAT("Studio Reference Pure", "PURE")
}

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMilliBels: Int // 100 milliBels = 1 dB, range -1200 to +1200 (-12dB to +12dB)
) {
    val levelDb: Float get() = levelMilliBels / 100f
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val isAiMode: Boolean = true,
    val activeAiTarget: AiAudioTarget = AiAudioTarget.AUTO_DETECT,
    val detectedProfileName: String = "Cinematic Soundstage",
    val aiRationaleTitle: String = "Cinematic Dynamic Soundstage",
    val aiRationaleDescription: String = "Enhanced deep sub-bass resonance and expanded spatial stereo width for grand orchestral dynamics and sweeping fidelity.",
    val currentPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, "60Hz", 0),
        EqualizerBand(1, 230, "230Hz", 0),
        EqualizerBand(2, 910, "910Hz", 0),
        EqualizerBand(3, 3600, "3.6kHz", 0),
        EqualizerBand(4, 14000, "14kHz", 0)
    ),
    val bassBoostStrength: Int = 0, // 0 to 1000 (0% - 100%)
    val virtualizerStrength: Int = 0, // 0 to 1000 (0% - 100%)
    val isHardwareAttached: Boolean = false
) {
    val bassBoostPercent: Int get() = (bassBoostStrength / 10).coerceIn(0, 100)
    val virtualizerPercent: Int get() = (virtualizerStrength / 10).coerceIn(0, 100)
}
