package com.example.auramusic.recommendation

import android.content.Context
import android.content.SharedPreferences
import com.example.auramusic.data.AudioRepository
import com.example.auramusic.model.Album
import com.example.auramusic.model.RecommendationSection
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import com.example.auramusic.network.OnlineMusicSearchService
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * High-precision semantic music vibes used for acoustic and stylistic clustering.
 */
enum class MusicVibe(val label: String, val chipName: String) {
    DEVOTIONAL("Devotional & Sacred", "Devotional"),
    ROMANTIC_SOULFUL("Romantic & Soulful", "Acoustic"),
    HIGH_ENERGY_DANCE("High Energy & Dance", "Bass Punch"),
    LOFI_ACOUSTIC_INDIE("Lo-Fi & Acoustic Indie", "Lofi Chill"),
    CINEMATIC_AMBIENT("Cinematic & Ambient", "Cinematic"),
    ROCK_ALTERNATIVE("Rock & Alternative", "Rock"),
    DEEP_FOCUS("Deep Focus & Study", "Focus & Study"),
    CHART_TOPPING_POP("Top Hits & Pop", "Top Hits")
}

/**
 * Contextual time-of-day listening persona.
 */
data class TimeContext(
    val id: String,
    val title: String,
    val subtitle: String,
    val preferredVibes: List<MusicVibe>,
    val source: StreamingSource
)

/**
 * Upgraded, intelligent, multi-dimensional Music Recommendation Engine for AuraMusic.
 * Features:
 * - Persistent behavioral user affinity modeling (plays, completions, skips)
 * - Multi-vector semantic similarity scoring (artist, language, vibe, tempo, quality)
 * - Chronobiological time-of-day adaptive feed generation
 * - Contextual "Sonic Neighbors" & "Made For You" algorithmic blending
 * - Intelligent Track Radio generation for infinite continuous playback
 * - Mood and vibe-based discovery clustering
 */
class RecommendationEngine(
    private val audioRepository: AudioRepository,
    context: Context? = null
) {

    private val prefs: SharedPreferences? = try {
        context?.getSharedPreferences("aura_recommendation_matrix_v2", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }

    // In-memory behavioral learning stores
    private val artistAffinityMap = ConcurrentHashMap<String, Float>()
    private val vibeAffinityMap = ConcurrentHashMap<MusicVibe, Float>()
    private val languageAffinityMap = ConcurrentHashMap<String, Float>()
    private val recentlyPlayedTrackIds = LinkedHashSet<String>()
    private val playCountMap = ConcurrentHashMap<String, Int>()

    init {
        loadPersistedAffinities()
    }

    // ==========================================
    // 1. BEHAVIORAL LISTENING EVENT TRACKING
    // ==========================================

    /**
     * Records when a user starts listening to a track.
     */
    fun recordPlaybackStart(track: Track) {
        val cleanArtist = extractPrimaryArtist(track.artist)
        if (cleanArtist.isNotBlank()) {
            artistAffinityMap[cleanArtist] = (artistAffinityMap[cleanArtist] ?: 0f) + 1.0f
        }

        val vibe = detectVibe(track)
        vibeAffinityMap[vibe] = (vibeAffinityMap[vibe] ?: 0f) + 0.8f

        val lang = detectLanguage(track.title, track.artist)
        languageAffinityMap[lang] = (languageAffinityMap[lang] ?: 0f) + 0.8f

        playCountMap[track.id] = (playCountMap[track.id] ?: 0) + 1

        synchronized(recentlyPlayedTrackIds) {
            recentlyPlayedTrackIds.remove(track.id)
            recentlyPlayedTrackIds.add(track.id)
            if (recentlyPlayedTrackIds.size > 50) {
                val oldest = recentlyPlayedTrackIds.first()
                recentlyPlayedTrackIds.remove(oldest)
            }
        }

        persistAffinities()
    }

    /**
     * Records a full track completion (listen ratio >= 75%).
     * Strongly reinforces artist, vibe, and cultural affinity.
     */
    fun recordPlaybackCompletion(track: Track) {
        val cleanArtist = extractPrimaryArtist(track.artist)
        if (cleanArtist.isNotBlank()) {
            artistAffinityMap[cleanArtist] = (artistAffinityMap[cleanArtist] ?: 0f) + 2.5f
        }

        val vibe = detectVibe(track)
        vibeAffinityMap[vibe] = (vibeAffinityMap[vibe] ?: 0f) + 2.0f

        val lang = detectLanguage(track.title, track.artist)
        languageAffinityMap[lang] = (languageAffinityMap[lang] ?: 0f) + 1.5f

        persistAffinities()
    }

    /**
     * Records a quick skip (< 25s).
     * Applies subtle decay so the engine avoids repeating tracks that induce skip fatigue.
     */
    fun recordSkip(track: Track, positionMs: Long, durationMs: Long) {
        if (positionMs < 25000L && durationMs > 60000L) {
            val cleanArtist = extractPrimaryArtist(track.artist)
            if (cleanArtist.isNotBlank()) {
                val cur = artistAffinityMap[cleanArtist] ?: 0f
                artistAffinityMap[cleanArtist] = (cur - 0.5f).coerceAtLeast(0f)
            }
            val vibe = detectVibe(track)
            val curVibe = vibeAffinityMap[vibe] ?: 0f
            vibeAffinityMap[vibe] = (curVibe - 0.4f).coerceAtLeast(0f)
        }
    }

    // ==========================================
    // 2. MULTI-VECTOR SIMILARITY SCORING
    // ==========================================

    /**
     * Computes a similarity metric [0.0..100.0] between a candidate track and a seed track.
     * Incorporates artist synergy, linguistic/regional match, vibe alignment, duration proximity,
     * and freshness adjustments.
     */
    fun computeSimilarityScore(candidate: Track, seed: Track): Float {
        if (candidate.id == seed.id) return 0f

        var score = 0f

        // 1. Artist Affinity (Exact or Collaborator match)
        val seedArtist = seed.artist.lowercase()
        val candArtist = candidate.artist.lowercase()
        val seedTokens = tokenizeArtist(seedArtist)
        val candTokens = tokenizeArtist(candArtist)

        if (seedTokens.any { candTokens.contains(it) }) {
            score += 45f // Direct artist or featured artist overlap
        } else if (seedTokens.any { st -> candTokens.any { ct -> ct.contains(st) || st.contains(ct) } }) {
            score += 25f
        }

        // 2. Linguistic & Regional Cultural Match
        val seedLang = detectLanguage(seed.title, seed.artist)
        val candLang = detectLanguage(candidate.title, candidate.artist)
        if (seedLang == candLang && seedLang != "Unknown") {
            score += 25f
        }

        // 3. Vibe / Genre Profile Alignment
        val seedVibe = detectVibe(seed)
        val candVibe = detectVibe(candidate)
        if (seedVibe == candVibe) {
            score += 20f
        } else if (areVibesComplementary(seedVibe, candVibe)) {
            score += 10f
        }

        // 4. Audiophile / Lossless Match
        if (candidate.isLossless && seed.isLossless) score += 5f

        // 5. Duration Proximity (smoother acoustic flow)
        val deltaSec = abs((candidate.durationMs - seed.durationMs) / 1000L)
        if (deltaSec <= 35) score += 5f

        // 6. Freshness Penalty (prevents listening loops)
        synchronized(recentlyPlayedTrackIds) {
            if (recentlyPlayedTrackIds.contains(candidate.id)) {
                score -= 15f
            }
        }

        return score.coerceIn(0f, 100f)
    }

    // ==========================================
    // 3. CHRONOBIOLOGICAL TIME CONTEXT
    // ==========================================

    /**
     * Detects the current time-of-day phase and returns appropriate contextual curation.
     */
    fun getTimeOfDayContext(): TimeContext {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimeContext(
                id = "rec_morning_flow",
                title = "Morning Flow & Awakening",
                subtitle = "Energizing acoustic melodies, uplifting vocals & morning clarity",
                preferredVibes = listOf(MusicVibe.DEVOTIONAL, MusicVibe.LOFI_ACOUSTIC_INDIE, MusicVibe.ROMANTIC_SOULFUL, MusicVibe.CHART_TOPPING_POP),
                source = StreamingSource.SPOTIFY
            )
            in 12..16 -> TimeContext(
                id = "rec_deep_focus",
                title = "Deep Focus & Afternoon Calm",
                subtitle = "Concentration soundscapes, acoustic resonance & calming rhythms",
                preferredVibes = listOf(MusicVibe.DEEP_FOCUS, MusicVibe.CINEMATIC_AMBIENT, MusicVibe.LOFI_ACOUSTIC_INDIE),
                source = StreamingSource.YOUTUBE_MUSIC
            )
            in 17..21 -> TimeContext(
                id = "rec_golden_hour",
                title = "Golden Hour Vibes & Anthems",
                subtitle = "Chart-topping party hits, dynamic bass & high-energy anthems",
                preferredVibes = listOf(MusicVibe.HIGH_ENERGY_DANCE, MusicVibe.ROCK_ALTERNATIVE, MusicVibe.CHART_TOPPING_POP),
                source = StreamingSource.SPOTIFY
            )
            else -> TimeContext(
                id = "rec_midnight_chill",
                title = "Midnight Melancholy & Chill",
                subtitle = "Soothing late-night acoustics, mellow soul & midnight tranquility",
                preferredVibes = listOf(MusicVibe.ROMANTIC_SOULFUL, MusicVibe.CINEMATIC_AMBIENT, MusicVibe.LOFI_ACOUSTIC_INDIE),
                source = StreamingSource.LOSSLESS_FLAC
            )
        }
    }

    // ==========================================
    // 4. MAIN DYNAMIC RECOMMENDATION FEED
    // ==========================================

    /**
     * Generates rich, multi-layered algorithmic recommendation sections for Home tab.
     * Incorporates:
     * - "Made For You • Daily Mix"
     * - Time-of-Day Adaptive Curation (Morning/Afternoon/Golden Hour/Midnight)
     * - "Sonic Neighbors" (More Like Current Track)
     * - "Trending on Spotify"
     * - "Hot on YouTube Music"
     * - "Lossless Masterworks"
     * - "Deep Cuts & Hidden Gems"
     */
    fun getOnlineRecommendationSections(
        activeTrack: Track?,
        providedTracks: List<Track> = emptyList(),
        providedAlbums: List<Album> = emptyList()
    ): List<RecommendationSection> {
        val allAlbums = when {
            providedAlbums.isNotEmpty() -> providedAlbums
            audioRepository.getAllAlbums().isNotEmpty() -> audioRepository.getAllAlbums()
            else -> curatedFallbackAlbums
        }

        val allTracks = when {
            providedTracks.isNotEmpty() -> providedTracks
            audioRepository.getMultiSourceTracks().isNotEmpty() -> audioRepository.getMultiSourceTracks()
            else -> curatedFallbackTracks
        }

        val sections = mutableListOf<RecommendationSection>()

        // -----------------------------------------------------------------
        // SECTION 1: Made For You • Daily Mix (Personalized Algorithmic Blend)
        // -----------------------------------------------------------------
        val topArtists = getTopArtists(3)
        val dailyMixTracks = rankTracksByUserAffinity(allTracks, activeTrack).take(6)
        val dailyMixAlbums = allAlbums.sortedByDescending { album ->
            val albumArtist = extractPrimaryArtist(album.artist)
            (artistAffinityMap[albumArtist] ?: 0f) + if (activeTrack != null && album.artist.contains(activeTrack.artist, ignoreCase = true)) 10f else 0f
        }.take(5)

        val dailyMixSubtitle = if (topArtists.isNotEmpty()) {
            "Curated around your love of ${topArtists.joinToString(", ")}"
        } else if (activeTrack != null) {
            "Sonic blend inspired by ${activeTrack.title}"
        } else {
            "Personalized lossless mix tuned to your tastes"
        }

        sections.add(
            RecommendationSection(
                id = "rec_daily_mix",
                title = "Made For You • Daily Mix",
                subtitle = dailyMixSubtitle,
                source = activeTrack?.source ?: StreamingSource.SPOTIFY,
                albums = dailyMixAlbums,
                tracks = dailyMixTracks
            )
        )

        // -----------------------------------------------------------------
        // SECTION 2: Chronobiological Context (Time of Day Adaptive)
        // -----------------------------------------------------------------
        val timeCtx = getTimeOfDayContext()
        val timeContextTracks = allTracks.filter { track ->
            val vibe = detectVibe(track)
            timeCtx.preferredVibes.contains(vibe)
        }.shuffled().take(6).ifEmpty { allTracks.shuffled().take(6) }

        val timeContextAlbums = allAlbums.filter { album ->
            timeCtx.source == album.source || album.isLossless
        }.take(4).ifEmpty { allAlbums.take(4) }

        sections.add(
            RecommendationSection(
                id = timeCtx.id,
                title = timeCtx.title,
                subtitle = timeCtx.subtitle,
                source = timeCtx.source,
                albums = timeContextAlbums,
                tracks = timeContextTracks
            )
        )

        // -----------------------------------------------------------------
        // SECTION 3: Sonic Neighbors ("More Like [Current Track]") or "Artist Spotlight"
        // -----------------------------------------------------------------
        if (activeTrack != null) {
            val sonicNeighbors = allTracks
                .filter { it.id != activeTrack.id }
                .map { Pair(it, computeSimilarityScore(it, activeTrack)) }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(6)

            val relatedAlbums = allAlbums.filter {
                it.artist.contains(activeTrack.artist, ignoreCase = true) || it.title.contains(activeTrack.album, ignoreCase = true)
            }.take(3).ifEmpty { allAlbums.filter { it.source == activeTrack.source }.take(3) }

            sections.add(
                RecommendationSection(
                    id = "rec_sonic_neighbors",
                    title = "More Like ${activeTrack.title}",
                    subtitle = "Sonic neighbors matching ${activeTrack.artist} • ${detectVibe(activeTrack).label}",
                    source = activeTrack.source,
                    albums = relatedAlbums,
                    tracks = sonicNeighbors
                )
            )
        }

        // -----------------------------------------------------------------
        // SECTION 4: Trending on Spotify (Chart-toppers in 320kbps)
        // -----------------------------------------------------------------
        val spotifyAlbums = allAlbums.filter { it.source == StreamingSource.SPOTIFY }.ifEmpty { allAlbums.take(4) }
        val spotifyTracks = allTracks.filter { it.source == StreamingSource.SPOTIFY }.ifEmpty { allTracks.take(5) }
        sections.add(
            RecommendationSection(
                id = "rec_spotify_trending",
                title = "Trending on Spotify",
                subtitle = "Popular 320kbps full-length studio streams",
                source = StreamingSource.SPOTIFY,
                albums = spotifyAlbums,
                tracks = spotifyTracks.take(5)
            )
        )

        // -----------------------------------------------------------------
        // SECTION 5: Hot on YouTube Music (Lossless & Live Master Releases)
        // -----------------------------------------------------------------
        val ytmAlbums = allAlbums.filter { it.source == StreamingSource.YOUTUBE_MUSIC }.ifEmpty { allAlbums.drop(1).take(4) }
        val ytmTracks = allTracks.filter { it.source == StreamingSource.YOUTUBE_MUSIC }.ifEmpty { allTracks.drop(2).take(5) }
        sections.add(
            RecommendationSection(
                id = "rec_ytm_hot",
                title = "Hot on YouTube Music",
                subtitle = "High-definition Opus master soundscapes & viral hits",
                source = StreamingSource.YOUTUBE_MUSIC,
                albums = ytmAlbums,
                tracks = ytmTracks.take(5)
            )
        )

        // -----------------------------------------------------------------
        // SECTION 6: Audiophile Lossless Masterworks
        // -----------------------------------------------------------------
        val losslessAlbums = allAlbums.filter { it.isLossless }.ifEmpty { allAlbums.take(3) }
        val losslessTracks = allTracks.filter { it.isLossless }.ifEmpty { allTracks.take(5) }
        sections.add(
            RecommendationSection(
                id = "rec_lossless_masters",
                title = "Lossless Masterworks",
                subtitle = "Studio master quality recordings in pure acoustic fidelity",
                source = StreamingSource.LOSSLESS_FLAC,
                albums = losslessAlbums,
                tracks = losslessTracks.take(5)
            )
        )

        // -----------------------------------------------------------------
        // SECTION 7: Deep Cuts & Undiscovered Gems (Zero/Low Play Count)
        // -----------------------------------------------------------------
        val deepCuts = allTracks.filter { (playCountMap[it.id] ?: 0) == 0 }
            .shuffled()
            .take(5)

        if (deepCuts.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    id = "rec_deep_cuts",
                    title = "Deep Cuts & Undiscovered Gems",
                    subtitle = "Explore hidden acoustic treasures from the global catalog",
                    source = StreamingSource.LOSSLESS_FLAC,
                    albums = emptyList(),
                    tracks = deepCuts
                )
            )
        }

        return sections
    }

    // ==========================================
    // 5. TRACK RADIO & INFINITE SMART QUEUE
    // ==========================================

    /**
     * Generates a coherent, non-repetitive smart radio queue from a seed track.
     * Evaluates harmonic compatibility, vibe continuity, and user affinity.
     */
    fun generateRadioQueue(seedTrack: Track, pool: List<Track>, count: Int = 15): List<Track> {
        val candidates = pool.filter { it.id != seedTrack.id }
        if (candidates.isEmpty()) return listOf(seedTrack)

        val scored = candidates.map { track ->
            val sim = computeSimilarityScore(track, seedTrack)
            val userBoost = (artistAffinityMap[extractPrimaryArtist(track.artist)] ?: 0f) * 2f
            Pair(track, sim + userBoost)
        }.sortedByDescending { it.second }

        val radioTracks = mutableListOf<Track>()
        radioTracks.add(seedTrack)

        // Select top matches with diversity injection
        for (item in scored) {
            if (radioTracks.size >= count) break
            radioTracks.add(item.first)
        }

        return radioTracks
    }

    /**
     * Finds related songs and albums for a specific track.
     */
    fun getRelatedContent(track: Track): Pair<List<Album>, List<Track>> {
        val allAlbums = audioRepository.getAllAlbums().ifEmpty { curatedFallbackAlbums }
        val allTracks = audioRepository.getMultiSourceTracks().ifEmpty { curatedFallbackTracks }

        val relatedAlbums = allAlbums.filter {
            it.source == track.source || it.artist.contains(track.artist, ignoreCase = true)
        }
        val relatedTracks = allTracks
            .filter { it.id != track.id }
            .map { Pair(it, computeSimilarityScore(it, track)) }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(10)

        return Pair(relatedAlbums, relatedTracks)
    }

    /**
     * Returns tracks matching a quick vibe/mood chip (e.g. Focus, Lofi, Cinematic, Rock, Devotional).
     */
    fun getTracksForMoodChip(chipName: String, pool: List<Track>): List<Track> {
        val allTracks = pool.ifEmpty { curatedFallbackTracks }
        val targetVibe = MusicVibe.entries.firstOrNull { it.chipName.equals(chipName, ignoreCase = true) }

        return if (targetVibe != null) {
            allTracks.filter { detectVibe(it) == targetVibe }
        } else {
            when (chipName.lowercase()) {
                "all recommendations", "all" -> allTracks
                "focus & study" -> allTracks.filter { detectVibe(it) == MusicVibe.DEEP_FOCUS || detectVibe(it) == MusicVibe.CINEMATIC_AMBIENT }
                "lofi chill" -> allTracks.filter { detectVibe(it) == MusicVibe.LOFI_ACOUSTIC_INDIE }
                "cinematic" -> allTracks.filter { detectVibe(it) == MusicVibe.CINEMATIC_AMBIENT }
                "bass punch" -> allTracks.filter { detectVibe(it) == MusicVibe.HIGH_ENERGY_DANCE }
                "acoustic" -> allTracks.filter { detectVibe(it) == MusicVibe.ROMANTIC_SOULFUL }
                "rock" -> allTracks.filter { detectVibe(it) == MusicVibe.ROCK_ALTERNATIVE }
                else -> allTracks
            }
        }
    }

    // ==========================================
    // 6. INTERNAL TAXONOMY & HELPER METHODS
    // ==========================================

    private fun rankTracksByUserAffinity(tracks: List<Track>, activeTrack: Track?): List<Track> {
        return tracks.map { track ->
            var weight = 0f

            // User artist affinity boost
            val primaryArtist = extractPrimaryArtist(track.artist)
            weight += (artistAffinityMap[primaryArtist] ?: 0f) * 3f

            // User vibe affinity boost
            val vibe = detectVibe(track)
            weight += (vibeAffinityMap[vibe] ?: 0f) * 2f

            // Language affinity boost
            val lang = detectLanguage(track.title, track.artist)
            weight += (languageAffinityMap[lang] ?: 0f) * 1.5f

            // Active track synergy
            if (activeTrack != null && track.id != activeTrack.id) {
                weight += computeSimilarityScore(track, activeTrack) * 0.5f
            }

            // Quality boost
            if (track.isLossless) weight += 5f

            Pair(track, weight)
        }.sortedByDescending { it.second }.map { it.first }
    }

    fun detectLanguage(title: String, artist: String): String {
        val text = "$title $artist".lowercase()
        return when {
            text.contains("telugu") || text.contains("anirudh") || text.contains("basinga") ||
            text.contains("yeshanagula") || text.contains("shyam") || text.contains("irumudi") ||
            text.contains("yaalalo") || text.contains("jangi reddy") || text.contains("prabha") ||
            text.contains("balaalu") -> "Telugu"

            text.contains("hindi") || text.contains("arijit") || text.contains("pritam") ||
            text.contains("armaan") || text.contains("shreya") || text.contains("jubin") ||
            text.contains("gehra hua") || text.contains("tum ho wahi") || text.contains("dil ibaadat") ||
            text.contains("barbaad") || text.contains("anuv jain") || text.contains("arz kiya") ||
            text.contains("kesariya") || text.contains("tauba") || text.contains("ram sampath") ||
            text.contains("himesh") || text.contains("saaj bhatt") || text.contains("awarapan") ||
            text.contains("mirzapur") || text.contains("vaaroon") -> "Hindi"

            text.contains("punjabi") || text.contains("honey singh") || text.contains("casa tupka") ||
            text.contains("diljit") || text.contains("karan aujla") || text.contains("sidhu") -> "Punjabi"

            text.contains("hanuman") || text.contains("chalisa") || text.contains("parvati") ||
            text.contains("shiv") || text.contains("shambhu") || text.contains("bholenath") ||
            text.contains("hariharan") -> "Devotional"

            text.contains("imagine dragons") || text.contains("believer") || text.contains("coldplay") ||
            text.contains("yellow") || text.contains("viva la vida") || text.contains("the weeknd") ||
            text.contains("starboy") || text.contains("zimmer") || text.contains("interstellar") ||
            text.contains("inception") || text.contains("time") -> "English"

            else -> "Global"
        }
    }

    fun detectVibe(track: Track): MusicVibe {
        val text = "${track.title} ${track.artist} ${track.album}".lowercase()
        return when {
            text.contains("hanuman") || text.contains("chalisa") || text.contains("parvati") ||
            text.contains("shambhu") || text.contains("bholenath") || text.contains("hariharan") ->
                MusicVibe.DEVOTIONAL

            text.contains("interstellar") || text.contains("inception") || text.contains("zimmer") ||
            text.contains("time") || text.contains("soundscape") || text.contains("theme") ->
                MusicVibe.CINEMATIC_AMBIENT

            text.contains("casa tupka") || text.contains("honey singh") || text.contains("tauba") ||
            text.contains("party") || text.contains("remix") || text.contains("dance") ->
                MusicVibe.HIGH_ENERGY_DANCE

            text.contains("arz kiya") || text.contains("anuv jain") || text.contains("radhimaa") ||
            text.contains("coke studio") || text.contains("lo-fi") || text.contains("lofi") ||
            text.contains("chill") ->
                MusicVibe.LOFI_ACOUSTIC_INDIE

            text.contains("tum ho wahi") || text.contains("dil ibaadat") || text.contains("barbaad") ||
            text.contains("gehra hua") || text.contains("kesariya") || text.contains("vaaroon") ||
            text.contains("tera mera") || text.contains("yaalalo") || text.contains("soul") ->
                MusicVibe.ROMANTIC_SOULFUL

            text.contains("imagine dragons") || text.contains("believer") || text.contains("coldplay") ||
            text.contains("viva la vida") || text.contains("yellow") || text.contains("rock") ->
                MusicVibe.ROCK_ALTERNATIVE

            text.contains("study") || text.contains("focus") || text.contains("instrumental") ->
                MusicVibe.DEEP_FOCUS

            else -> MusicVibe.CHART_TOPPING_POP
        }
    }

    private fun areVibesComplementary(v1: MusicVibe, v2: MusicVibe): Boolean {
        return (v1 == MusicVibe.ROMANTIC_SOULFUL && v2 == MusicVibe.LOFI_ACOUSTIC_INDIE) ||
               (v1 == MusicVibe.LOFI_ACOUSTIC_INDIE && v2 == MusicVibe.DEEP_FOCUS) ||
               (v1 == MusicVibe.DEEP_FOCUS && v2 == MusicVibe.CINEMATIC_AMBIENT) ||
               (v1 == MusicVibe.HIGH_ENERGY_DANCE && v2 == MusicVibe.CHART_TOPPING_POP) ||
               (v1 == MusicVibe.ROCK_ALTERNATIVE && v2 == MusicVibe.CHART_TOPPING_POP)
    }

    private fun extractPrimaryArtist(artist: String): String {
        return artist.split(",", "&", "feat.", "ft.", "/").firstOrNull()?.trim() ?: artist.trim()
    }

    private fun tokenizeArtist(artist: String): List<String> {
        return artist.split(",", "&", "feat.", "ft.", "/", "-")
            .map { it.trim().lowercase() }
            .filter { it.length > 2 }
    }

    fun getTopArtists(limit: Int = 3): List<String> {
        return artistAffinityMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    private fun persistAffinities() {
        prefs ?: return
        try {
            val artistJson = JSONObject()
            artistAffinityMap.forEach { (k, v) -> artistJson.put(k, v.toDouble()) }

            val vibeJson = JSONObject()
            vibeAffinityMap.forEach { (k, v) -> vibeJson.put(k.name, v.toDouble()) }

            val langJson = JSONObject()
            languageAffinityMap.forEach { (k, v) -> langJson.put(k, v.toDouble()) }

            prefs.edit()
                .putString("user_artists", artistJson.toString())
                .putString("user_vibes", vibeJson.toString())
                .putString("user_languages", langJson.toString())
                .apply()
        } catch (e: Exception) {
            // Ignore persistence errors
        }
    }

    private fun loadPersistedAffinities() {
        prefs ?: return
        try {
            prefs.getString("user_artists", null)?.let { jsonStr ->
                val obj = JSONObject(jsonStr)
                obj.keys().forEach { key ->
                    artistAffinityMap[key] = obj.optDouble(key, 0.0).toFloat()
                }
            }
            prefs.getString("user_vibes", null)?.let { jsonStr ->
                val obj = JSONObject(jsonStr)
                obj.keys().forEach { key ->
                    try {
                        val vibe = MusicVibe.valueOf(key)
                        vibeAffinityMap[vibe] = obj.optDouble(key, 0.0).toFloat()
                    } catch (ignored: Exception) {}
                }
            }
            prefs.getString("user_languages", null)?.let { jsonStr ->
                val obj = JSONObject(jsonStr)
                obj.keys().forEach { key ->
                    languageAffinityMap[key] = obj.optDouble(key, 0.0).toFloat()
                }
            }
        } catch (e: Exception) {
            // Ignore corrupt prefs
        }
    }

    // ==========================================
    // 7. COMPREHENSIVE CURATED FALLBACK BANK
    // ==========================================

    private val curatedFallbackTracks by lazy {
        listOf(
            Track(
                id = "curated_tum_ho_wahi",
                title = "Tum Ho Wahi",
                artist = "Armaan Malik, Shreya Karmakar, Ram Sampath",
                album = "Don't Be Shy",
                durationMs = 214000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/974/Don-t-Be-Shy-Hindi-2026-20260912180512-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_dil_ibaadat",
                title = "Dil Ibaadat",
                artist = "KK, Pritam",
                album = "Tum Mile",
                durationMs = 329000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyoW+9vqQ1AvpNtBuxtRV/trXO0wRsM9SJAudwVBa8NXR0K5rNf9PdHhw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/184/Tum-Mile-Hindi-2009-20190629162908-500x500.jpg",
                source = StreamingSource.LOSSLESS_FLAC,
                qualityBadge = "24-BIT FLAC",
                isLossless = true,
                category = "Lossless"
            ),
            Track(
                id = "curated_barbaad",
                title = "Barbaad",
                artist = "Jubin Nautiyal, The Rish",
                album = "Saiyaara",
                durationMs = 213000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDynTibb8fRyG1tbcCQQQR3MOwq4OxvkVNxRxh5wdnqMsXr4IMM0hwwihw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/582/Saiyaara-Hindi-2025-20250711153018-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_arz_kiya_hai",
                title = "Arz Kiya Hai",
                artist = "Anuv Jain, Lost Stories",
                album = "Coke Studio Bharat",
                durationMs = 289000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy0sB13nYLqBDMTtFUYrKmMui3nVSjruDwLS/IRFPQXAJAhv8jbZTiwRw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/839/Arz-Kiya-Hai-Coke-Studio-Bharat-Hindi-2025-20250808124500-500x500.jpg",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "YT LOSSLESS",
                isLossless = true,
                category = "YouTube Music"
            ),
            Track(
                id = "curated_gehra_hua",
                title = "Gehra Hua",
                artist = "Arijit Singh, Shashwat Sachdev",
                album = "Dhurandhar",
                durationMs = 245000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/391/Dhurandhar-Hindi-2026-20260905141012-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_yeshanagula",
                title = "Yeshanagula",
                artist = "Anirudh Ravichander, Singer Prabha, Jangi Reddy",
                album = "The Paradise",
                durationMs = 236000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy0sB13nYLqBDMTtFUYrKmMui3nVSjruDwLS/IRFPQXAJAhv8jbZTiwRw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/712/The-Paradise-Telugu-2026-20260901112015-500x500.jpg",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "YT LOSSLESS",
                isLossless = true,
                category = "YouTube Music"
            ),
            Track(
                id = "curated_basinga_balaalu",
                title = "Basinga Balaalu",
                artist = "Kalyan Keys, Swamy Naresh, Srinidhi Nerella",
                album = "Basinga Balaalu",
                durationMs = 256000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDynTibb8fRyG1tbcCQQQR3MOwq4OxvkVNxRxh5wdnqMsXr4IMM0hwwihw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/643/Basinga-Balaalu-Telugu-2026-20260828091522-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_hanuman_chalisa",
                title = "Shree Hanuman Chalisa",
                artist = "Hariharan, Lalit Sen",
                album = "Shree Hanuman Chalisa",
                durationMs = 582000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyoW+9vqQ1AvpNtBuxtRV/trXO0wRsM9SJAudwVBa8NXR0K5rNf9PdHhw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/023/Shree-Hanuman-Chalisa-Hindi-1992-20200810142010-500x500.jpg",
                source = StreamingSource.LOSSLESS_FLAC,
                qualityBadge = "24-BIT FLAC",
                isLossless = true,
                category = "Lossless"
            ),
            Track(
                id = "curated_casa_tupka",
                title = "Casa Tupka Anthemo",
                artist = "Yo Yo Honey Singh, Priyanshi Srivastava",
                album = "Casa Tupka Anthemo",
                durationMs = 184000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/481/Casa-Tupka-Anthemo-Hindi-2026-20260919103011-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_starboy",
                title = "Starboy",
                artist = "The Weeknd, Daft Punk",
                album = "Starboy",
                durationMs = 230000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDynTibb8fRyG1tbcCQQQR3MOwq4OxvkVNxRxh5wdnqMsXr4IMM0hwwihw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/410/Starboy-English-2016-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_believer",
                title = "Believer",
                artist = "Imagine Dragons",
                album = "Evolve",
                durationMs = 204000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/248/Evolve-English-2018-20260605220036-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_time",
                title = "Time",
                artist = "Hans Zimmer",
                album = "Inception (OST)",
                durationMs = 275000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy0sB13nYLqBDMTtFUYrKmMui3nVSjruDwLS/IRFPQXAJAhv8jbZTiwRw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/139/Inception-English-2010-500x500.jpg",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "YT LOSSLESS",
                isLossless = true,
                category = "YouTube Music"
            )
        )
    }

    private val curatedFallbackAlbums by lazy {
        listOf(
            Album(
                id = "curated_album_dont_be_shy",
                title = "Don't Be Shy",
                artist = "Ram Sampath, Armaan Malik",
                coverArtUrl = "https://c.saavncdn.com/974/Don-t-Be-Shy-Hindi-2026-20260912180512-500x500.jpg",
                year = "2026",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                description = "Romantic studio soundtrack featuring Tum Ho Wahi and Dil Ke Mehmaan.",
                tracks = curatedFallbackTracks.take(1)
            ),
            Album(
                id = "curated_album_tum_mile",
                title = "Tum Mile (Original Motion Picture Soundtrack)",
                artist = "Pritam, KK",
                coverArtUrl = "https://c.saavncdn.com/184/Tum-Mile-Hindi-2009-20190629162908-500x500.jpg",
                year = "2009",
                source = StreamingSource.LOSSLESS_FLAC,
                qualityBadge = "24-BIT FLAC",
                isLossless = true,
                description = "Audiophile cult classic featuring Dil Ibaadat, Tu Hi Haqeeqat and Tum Mile.",
                tracks = curatedFallbackTracks.drop(1).take(1)
            ),
            Album(
                id = "curated_album_the_paradise",
                title = "The Paradise",
                artist = "Anirudh Ravichander",
                coverArtUrl = "https://c.saavncdn.com/712/The-Paradise-Telugu-2026-20260901112015-500x500.jpg",
                year = "2026",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "YT LOSSLESS",
                isLossless = true,
                description = "Chart-shattering Telugu soundscapes featuring Yeshanagula and Aaya Sher.",
                tracks = curatedFallbackTracks.filter { it.title == "Yeshanagula" }
            ),
            Album(
                id = "curated_album_evolve",
                title = "Evolve",
                artist = "Imagine Dragons",
                coverArtUrl = "https://c.saavncdn.com/248/Evolve-English-2018-20260605220036-500x500.jpg",
                year = "2018",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                description = "Multi-platinum studio album featuring Believer and Whatever It Takes.",
                tracks = curatedFallbackTracks.filter { it.title == "Believer" }
            ),
            Album(
                id = "curated_album_inception",
                title = "Inception (Original Soundtrack)",
                artist = "Hans Zimmer",
                coverArtUrl = "https://c.saavncdn.com/139/Inception-English-2010-500x500.jpg",
                year = "2010",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "HD OPUS 256K",
                isLossless = true,
                description = "Iconic cinematic masterpiece featuring Time and Dream Is Collapsing.",
                tracks = curatedFallbackTracks.filter { it.title == "Time" }
            )
        )
    }
}
