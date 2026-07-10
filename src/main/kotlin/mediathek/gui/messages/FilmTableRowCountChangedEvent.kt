package mediathek.gui.messages

data class FilmTableRowCountChangedEvent(
    val rowCount: Int,
) : BaseEvent()
