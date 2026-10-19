package com.example.auramusic.player

object AudioQuality {
    fun describe(mimeType: String?, bitrate: Int, sampleRate: Int): String {
        val codec = when (mimeType) {
            "audio/flac" -> "FLAC"
            "audio/raw" -> "PCM"
            "audio/mp4a-latm" -> "AAC"
            "audio/mpeg" -> "MP3"
            "audio/opus" -> "Opus"
            "audio/vorbis" -> "Vorbis"
            else -> "Audio"
        }
        return listOfNotNull(codec, if (bitrate > 0) "${bitrate / 1000} kbps" else null,
            if (sampleRate > 0) "$sampleRate Hz" else null).joinToString(" · ")
    }
}
