package com.example.auramusic.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMusicSearchServiceTest {

    @Test
    fun decryptMediaUrl_decryptsKnownSaavnPayload() {
        val encrypted = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"
        val decrypted = OnlineMusicSearchService.decryptMediaUrl(encrypted)
        
        assertTrue("Decrypted URL should start with https://aac.saavncdn.com", decrypted.startsWith("https://aac.saavncdn.com/"))
        assertTrue("URL should be upgraded to 320kbps", decrypted.endsWith("_320.mp4"))
    }

    @Test
    fun cleanText_replacesHtmlEntities() {
        val input = "&quot;Hello &amp; Welcome&#039;s World&quot;"
        val cleaned = OnlineMusicSearchService.cleanText(input)
        assertEquals("\"Hello & Welcome's World\"", cleaned)
    }

    @Test
    fun decryptMediaUrl_returnsEmptyOnInvalidInput() {
        val result = OnlineMusicSearchService.decryptMediaUrl("invalid_garbage")
        assertEquals("", result)
    }
}
