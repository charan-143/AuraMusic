package com.example.auramusic.player

import org.junit.Assert.*
import org.junit.Test

class AudioQualityTest {
    @Test fun aacNeverClaimsLossless() {
        assertEquals("AAC · 320 kbps · 44100 Hz", AudioQuality.describe("audio/mp4a-latm", 320000, 44100))
    }
    @Test fun unknownFormatDoesNotInventQuality() {
        assertEquals("Audio", AudioQuality.describe(null, -1, -1))
    }
    @Test fun flacUsesActualSampleRate() {
        assertEquals("FLAC · 48000 Hz", AudioQuality.describe("audio/flac", -1, 48000))
    }
}
