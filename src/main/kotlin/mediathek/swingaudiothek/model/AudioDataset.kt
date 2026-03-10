package mediathek.swingaudiothek.model

import java.time.LocalDateTime

data class AudioDataset(
    val metaLocal: LocalDateTime?,
    val sourceUrl: String,
    val entries: List<AudioEntry>
)
