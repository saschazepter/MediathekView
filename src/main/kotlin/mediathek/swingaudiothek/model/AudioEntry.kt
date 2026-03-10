package mediathek.swingaudiothek.model

import java.net.URI
import java.time.LocalDateTime

data class AudioEntry(
    val channel: String,
    val genre: String,
    val theme: String,
    val title: String,
    val durationMinutes: Int?,
    val sizeMb: Int?,
    val description: String,
    val audioUrl: URI?,
    val websiteUrl: URI?,
    val isNew: Boolean,
    val isPodcast: Boolean,
    val isDuplicate: Boolean,
    val publishedAt: LocalDateTime?
)
