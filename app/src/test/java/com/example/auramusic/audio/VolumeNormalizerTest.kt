package com.example.auramusic.audio

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class VolumeNormalizerTest {
    private fun normalizedRms(amplitude: Float): Double {
        val normalizer = VolumeNormalizer()
        var power = 0.0
        val frame = FloatArray(2)
        for (i in 0 until 8000 * 8) {
            val sample = (amplitude * sin(2 * PI * 440 * i / 8000)).toFloat()
            frame[0] = sample; frame[1] = sample
            normalizer.processFrame(frame, 8000)
            assertTrue(abs(frame[0]) <= 0.98001f)
            assertEquals(frame[0], frame[1])
            if (i >= 8000 * 7) power += frame[0] * frame[0]
        }
        return sqrt(power / 8000)
    }
    @Test fun reducesLoudnessDifferenceAcrossSongs() {
        val quiet = normalizedRms(0.12f)
        val loud = normalizedRms(0.8f)
        assertTrue("Normalized levels should converge: $quiet vs $loud", abs(quiet - loud) < 0.02)
        assertTrue(loud < 0.2)
    }
    @Test fun silenceRemainsSilent() {
        val processor = VolumeNormalizer()
        val frame = FloatArray(2)
        repeat(16000) { processor.processFrame(frame, 8000) }
        assertEquals(0f, frame[0], 0f)
        assertEquals(0f, frame[1], 0f)
    }
    @Test fun suddenPeaksDoNotClip() {
        val frame = floatArrayOf(1f, -1f)
        VolumeNormalizer().processFrame(frame, 44100)
        assertTrue(frame.all { abs(it) <= 0.98f })
    }
}
