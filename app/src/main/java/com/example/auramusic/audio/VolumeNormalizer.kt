package com.example.auramusic.audio

import kotlin.math.exp
import kotlin.math.sqrt

/** Linked-channel RMS gain control, with a silence gate, bounded gain and peak protection. */
class VolumeNormalizer {
    private var energy = 0.015625
    private var gain = 1.0
    private var lastRate = 0
    private var energyRate = 0.0
    private var attackRate = 0.0
    private var releaseRate = 0.0
    fun reset() { energy = 0.015625; gain = 1.0 }

    fun processFrame(samples: FloatArray, sampleRate: Int) {
        if (lastRate != sampleRate) {
            lastRate = sampleRate
            energyRate = 1.0 - exp(-1.0 / (sampleRate * 0.4))
            attackRate = 1.0 - exp(-1.0 / (sampleRate * 0.02))
            releaseRate = 1.0 - exp(-1.0 / sampleRate)
        }
        var power = 0.0
        var peak = 0.0
        for (sample in samples) { power += sample * sample; peak = maxOf(peak, kotlin.math.abs(sample.toDouble())) }
        energy += energyRate * (power / samples.size - energy)
        val rms = sqrt(energy)
        val desired = if (rms < 0.002) 1.0 else (0.125 / rms).coerceIn(0.1, 2.0)
        val rate = if (desired < gain) attackRate else releaseRate
        gain += rate * (desired - gain)
        val safeGain = minOf(gain, if (peak > 0.0) 0.98 / peak else gain)
        for (i in samples.indices) samples[i] = (samples[i] * safeGain).toFloat().coerceIn(-0.98f, 0.98f)
    }
}
