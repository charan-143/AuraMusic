package com.example.auramusic.data

import com.example.auramusic.model.*
import com.example.auramusic.network.CatalogTrackResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TrackMappingTest {
    private val old = Track("curated_yeshanagula", "Yeshanagula", "Anirudh Ravichander, Singer Prabha, Jangi Reddy",
        album="The Paradise",audioUrl="https://wrong.example/arz.mp4",localFilePath="/old/wrong.mp4",isCachedOffline=true)
    private val correct = Track("saavn_song_gQsrqA4C", "Yeshanagula", "Anirudh Ravichander, Singer Prabha, Jangi Reddy, Kasarla Shyam",
        album="The Paradise (Telugu)",audioUrl="https://right.example/yeshanagula.mp4",source=StreamingSource.JIOSAAVN)

    @Test fun selectsMatchingRecordingInsteadOfFirstSearchResult() {
        val wrongArtist=correct.copy(id="saavn_song_other",artist="Scarlett Ferrari",album="Gehra Hua Brazil",audioUrl="https://wrong.example/other.mp4")
        val repaired=requireNotNull(TrackMapping.matchingTrack(old,listOf(wrongArtist,correct)))
        assertEquals(old.id,repaired.id)
        assertEquals(correct.id,repaired.resolvedCatalogId)
        assertEquals(correct.audioUrl,repaired.audioUrl)
        assertFalse(repaired.isCachedOffline)
        assertNull(repaired.localFilePath)
    }
    @Test fun unrelatedTitleCannotSupplyAudio() {
        assertNull(TrackMapping.matchingTrack(old,listOf(correct.copy(title="Arz Kiya Hai"))))
    }
    @Test fun fromAlbumSuffixMatchesButRemixTitleDoesNot() {
        assertNotNull(TrackMapping.matchingTrack(old,listOf(correct.copy(title="Yeshanagula (From \"The Paradise\") (Telugu)"))))
        assertNull(TrackMapping.matchingTrack(old,listOf(correct.copy(title="Yeshanagula (Epic Orchestra)"))))
    }
    @Test fun exactAlbumPreferredOverSingle() {
        val single=correct.copy(id="saavn_song_single",title="Yeshanagula (From \"The Paradise\") (Telugu)",album="Yeshanagula")
        assertEquals(correct.id,TrackMapping.matchingTrack(old,listOf(single,correct))?.resolvedCatalogId)
    }
    @Test fun unsafeLegacyPointersAreClearedButVerifiedMappingsSurvive() {
        val sanitized=TrackMapping.sanitize(old)
        assertEquals("",sanitized.audioUrl)
        assertNull(sanitized.localFilePath)
        assertFalse(sanitized.isCachedOffline)
        val repaired=requireNotNull(TrackMapping.matchingTrack(old,listOf(correct)))
        assertEquals(repaired,TrackMapping.sanitize(repaired))
        assertEquals(correct,TrackMapping.sanitize(correct))
    }
    @Test fun resolverCachesSuccessfulMatchButNeverAnUnrelatedFallback() = runBlocking {
        var searches=0
        val resolver=CatalogTrackResolver { searches++; listOf(correct) }
        assertEquals(correct.audioUrl,resolver.resolve(old)?.audioUrl)
        assertEquals(correct.audioUrl,resolver.resolve(old)?.audioUrl)
        assertEquals(1,searches)
        val unavailable=CatalogTrackResolver {listOf(correct.copy(title="Arz Kiya Hai"))}
        assertNull(unavailable.resolve(old))
    }
}
