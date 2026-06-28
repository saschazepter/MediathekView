package mediathek.gui.actions.export

import mediathek.config.Daten
import javax.swing.JFrame

class ExportDecompressedFilmlistAction(daten: Daten, parent: JFrame) : AbstractExportFilmlistAction(
    actionName = "Dekomprimierte Filmliste...",
    daten = daten,
    saveDialogTitle = "Lesbare Filmliste sichern",
    exportSettings = FilmlistExportSettings(
        compressSender = false,
        compressThema = false
    ),
    parent = parent,
)
