package mediathek.gui.bookmark

import mediathek.config.Daten

class BookmarkServices(daten: Daten) {
    val list: BookmarkDataList = BookmarkDataList(daten)

    fun loadFromFile() {
        list.loadFromFile()
    }

    fun saveToFile() {
        list.saveToFile()
    }
}
