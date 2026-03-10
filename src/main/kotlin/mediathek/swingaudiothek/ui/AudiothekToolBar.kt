package mediathek.swingaudiothek.ui

import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.JToolBar

class AudiothekToolBar : JToolBar() {
    private val searchField = JTextField(28)

    init {
        isFloatable = false

        add(JLabel("Filter"))
        addSeparator()
        add(searchField)
    }

    fun addFilterSubmitListener(action: (String) -> Unit) {
        searchField.addActionListener { action(searchField.text) }
    }

    fun setLoading(loading: Boolean) {
        searchField.isEnabled = !loading
    }

    fun currentQuery(): String = searchField.text
}
