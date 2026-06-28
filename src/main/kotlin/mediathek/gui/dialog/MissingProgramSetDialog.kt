/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.gui.dialog

import mediathek.config.Daten
import mediathek.config.Konstanten
import mediathek.daten.ListePsetVorlagen
import mediathek.tool.GuiFunktionenProgramme
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane

object MissingProgramSetDialog {
    private const val IMPORT_OPTION_INDEX = 0
    private const val CLOSE_OPTION_INDEX = 1

    private val options = arrayOf("Standardsets importieren", "Schließen")

    fun ensureAboProgramSetAvailable(parent: JFrame?, daten: Daten): Boolean {
        if (hasAboProgramSet(daten)) {
            return true
        }

        showMissingAboProgramSet(parent, daten)
        return hasAboProgramSet(daten)
    }

    fun showMissingAboProgramSet(parent: JFrame?, daten: Daten) {
        showMissingProgramSetIfNeeded(parent, daten, ::hasAboProgramSet, ::createAboMessageLabel)
    }

    fun showMissingDownloadProgramSet(parent: JFrame?, daten: Daten) {
        showMissingProgramSetIfNeeded(parent, daten, ::hasDownloadProgramSet, ::createDownloadMessageLabel)
    }

    private fun showMissingProgramSetIfNeeded(
        parent: JFrame?,
        daten: Daten,
        hasProgramSet: (Daten) -> Boolean,
        createMessageLabel: () -> JLabel,
    ) {
        if (hasProgramSet(daten)) {
            return
        }

        if (showImportPrompt(parent, createMessageLabel()) == IMPORT_OPTION_INDEX) {
            importStandardProgramSets(parent, daten)
        }
    }

    private fun hasAboProgramSet(daten: Daten): Boolean =
        daten.listePset.hasAboProgramSet()

    private fun hasDownloadProgramSet(daten: Daten): Boolean =
        daten.listePset.hasDownloadProgramSet()

    private fun importStandardProgramSets(parent: JFrame?, daten: Daten) {
        GuiFunktionenProgramme.addSetVorlagen(
            parent,
            daten,
            ListePsetVorlagen.getStandarset(parent, true),
            true,
        )
    }

    private fun showImportPrompt(parent: JFrame?, messageLabel: JLabel): Int =
        JOptionPane.showOptionDialog(
            parent,
            messageLabel,
            Konstanten.PROGRAMMNAME,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[CLOSE_OPTION_INDEX],
        )

    private fun createAboMessageLabel(): JLabel =
        JLabel(
            "<html>" +
                "Ein Set von Programmen zum Aufzeichnen wurde nicht angelegt.<br>" +
                "<br>" +
                "Im Menü unter:<br>" +
                "&quot;Datei-&gt;Einstellungen-&gt;Aufzeichnen und Abspielen&quot;<br>" +
                "ein Programm zum Aufzeichnen für Abos festlegen.<br>" +
                "Oder die Standardsets importieren." +
                "</html>"
        )

    private fun createDownloadMessageLabel(): JLabel =
        JLabel(
            "<html>" +
                "Ein Set von Programmen zum Speichern wurde nicht angelegt.<br>" +
                "<br>" +
                "Im Menü unter:<br>" +
                "&quot;Datei-&gt;Einstellungen-&gt;Aufzeichnen und Abspielen&quot;<br>" +
                "ein Programm zum Speichern festlegen.<br>" +
                "Oder die Standardsets importieren." +
                "</html>"
        )
}
