package mediathek.tool

import ca.odell.glazedlists.swing.DefaultEventComboBoxModel
import mediathek.config.Daten

class SenderListComboBoxModel(daten: Daten) : DefaultEventComboBoxModel<String>(
    EventListWithEmptyFirstEntry(daten.allSendersList)
) {
    init {
        selectedItem = ""
    }
}
