package com.example.auramusic.audio

import com.example.auramusic.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerEngineTest {

    @Test
    fun testAiEqualizer_detectsCinematicSoundtrack() {
        val track = Track(
            id = "hz1",
            title = "Time",
            artist = "Hans Zimmer",
            album = "Inception (Original Motion Picture Soundtrack)",
            isLossless = true
        )

        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.AUTO_DETECT,
            isLossless = true,
            isDataSaver = false
        )

        assertEquals("Cinematic Soundstage", result.profileName)
        assertTrue(result.rationaleTitle.contains("Soundtrack"))
        assertEquals(400, result.bandLevels[0]) // +4.0 dB at 60Hz
        assertEquals(750, result.virtualizer) // 75% 3D spatial widening
        assertEquals(350, result.bassBoost)
    }

    @Test
    fun testAiEqualizer_detectsElectronicDance() {
        val track = Track(
            id = "edm1",
            title = "One More Time",
            artist = "Daft Punk",
            album = "Discovery",
            isLossless = true
        )

        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.AUTO_DETECT,
            isLossless = true,
            isDataSaver = false
        )

        assertEquals("Sub-Bass & Sparkle", result.profileName)
        assertTrue(result.rationaleTitle.contains("Electronic"))
        assertEquals(650, result.bandLevels[0]) // +6.5 dB sub-bass
        assertEquals(650, result.bassBoost)
    }

    @Test
    fun testAiEqualizer_detectsVocalPop() {
        val track = Track(
            id = "voc1",
            title = "Easy On Me",
            artist = "Adele",
            album = "30",
            isLossless = true
        )

        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.AUTO_DETECT,
            isLossless = true,
            isDataSaver = false
        )

        assertEquals("Vocal Clarity Lift", result.profileName)
        assertTrue(result.rationaleTitle.contains("Vocal"))
        assertEquals(-150, result.bandLevels[0]) // -1.5 dB low cut
        assertEquals(450, result.bandLevels[3]) // +4.5 dB at 3.6kHz vocal presence
    }

    @Test
    fun testAiEqualizer_explicitTargetOverridesMetadata() {
        val track = Track(
            id = "t1",
            title = "Heavy Metal Rocker",
            artist = "Metallica",
            album = "Master",
            isLossless = true
        )

        // User explicitly wants Vocal Presence
        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.VOCAL_PRESENCE,
            isLossless = true,
            isDataSaver = false
        )

        assertEquals("Vocal Presence", result.profileName)
        assertEquals(350, result.bandLevels[2]) // 910Hz boost
        assertEquals(450, result.bandLevels[3]) // 3.6kHz boost
    }

    @Test
    fun testAiEqualizer_studioFlatReturnsZeroColoration() {
        val track = Track(
            id = "pure1",
            title = "Symphony No. 5",
            artist = "Beethoven",
            album = "Master Recordings",
            isLossless = true
        )

        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.STUDIO_FLAT,
            isLossless = true,
            isDataSaver = false
        )

        assertEquals("Studio Reference Pure", result.profileName)
        assertEquals(listOf(0, 0, 0, 0, 0), result.bandLevels)
        assertEquals(0, result.bassBoost)
        assertEquals(0, result.virtualizer)
    }

    @Test
    fun testAiEqualizer_dataSaverRollsOffHighs() {
        val track = Track(
            id = "norm1",
            title = "Standard Track",
            artist = "Unknown Indie",
            isLossless = false
        )

        val result = AiEqualizerEngine.analyze(
            track = track,
            target = AiAudioTarget.AUTO_DETECT,
            isLossless = false,
            isDataSaver = true
        )

        assertEquals("Adaptive Compression Shield", result.profileName)
        assertTrue(result.bandLevels[4] <= 0) // Rolled off 14kHz to mask compression artifact sizzle
        assertTrue(result.bandLevels[1] > 0) // Warmth boost
    }

    @Test
    fun testEqualizerBand_levelDbConversion() {
        val bandPos = EqualizerBand(0, 60, "60Hz", 450)
        assertEquals(4.5f, bandPos.levelDb, 0.01f)

        val bandNeg = EqualizerBand(1, 230, "230Hz", -300)
        assertEquals(-3.0f, bandNeg.levelDb, 0.01f)

        val bandZero = EqualizerBand(2, 910, "910Hz", 0)
        assertEquals(0.0f, bandZero.levelDb, 0.01f)
    }
}
