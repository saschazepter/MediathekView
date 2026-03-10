package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.model.AudioEntry
import java.time.format.DateTimeFormatter

enum class AudioTableColumn(
    val title: String,
    val preferredWidth: Int? = null,
    val centered: Boolean = false,
    val searchField: String? = null,
    val editable: Boolean = false,
    val valueProvider: (AudioEntry) -> Any
) {
    SENDER(
        title = "Sender",
        preferredWidth = 90,
        centered = true,
        searchField = AudiothekLuceneIndex.FIELD_SENDER,
        valueProvider = { it.channel }
    ),
    GENRE(
        title = "Genre",
        preferredWidth = 150,
        searchField = AudiothekLuceneIndex.FIELD_GENRE,
        valueProvider = { it.genre }
    ),
    THEME(
        title = "Thema",
        preferredWidth = 210,
        searchField = AudiothekLuceneIndex.FIELD_THEME,
        valueProvider = { it.theme }
    ),
    TITLE(
        title = "Titel",
        preferredWidth = 430,
        searchField = AudiothekLuceneIndex.FIELD_TITLE,
        valueProvider = { it.title }
    ),
    PLAY(
        title = "",
        preferredWidth = 32,
        editable = true,
        valueProvider = { "A" }
    ),
    DOWNLOAD(
        title = "",
        preferredWidth = 32,
        editable = true,
        valueProvider = { "D" }
    ),
    DATE(
        title = "Datum",
        preferredWidth = 110,
        centered = true,
        searchField = AudiothekLuceneIndex.FIELD_DATE,
        valueProvider = { it.publishedAt?.format(DATE_FORMAT).orEmpty() }
    ),
    TIME(
        title = "Zeit",
        preferredWidth = 80,
        centered = true,
        searchField = AudiothekLuceneIndex.FIELD_TIME,
        valueProvider = { it.publishedAt?.format(TIME_FORMAT).orEmpty() }
    ),
    DURATION(
        title = "Dauer",
        preferredWidth = 80,
        centered = true,
        searchField = AudiothekLuceneIndex.FIELD_DURATION,
        valueProvider = { it.durationMinutes?.toString().orEmpty() }
    ),
    SIZE(
        title = "Größe",
        preferredWidth = 80,
        centered = true,
        searchField = AudiothekLuceneIndex.FIELD_SIZE,
        valueProvider = { it.sizeMb?.toString().orEmpty() }
    );

    val modelIndex: Int
        get() = ordinal

    val toggleable: Boolean
        get() = searchField != null

    companion object {
        val searchableColumns: List<AudioTableColumn>
            get() = entries.filter(AudioTableColumn::toggleable)

        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
