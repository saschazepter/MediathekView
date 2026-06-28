package mediathek.gui.actions.export

import mediathek.config.Daten
import javax.swing.JFrame

class ExportReadableFilmlistAction(daten: Daten, parent: JFrame) : AbstractExportFilmlistAction(
    actionName = "Lesbare Filmliste...",
    daten = daten,
    saveDialogTitle = "Lesbare Filmliste sichern",
    exportSettings = FilmlistExportSettings(
        compressSender = true,
        compressThema = true
    ),
    parent = parent,
)
