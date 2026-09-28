package com.example.auramusic.model

data class RecommendationSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val source: StreamingSource,
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList()
)
