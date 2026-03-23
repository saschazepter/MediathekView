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

package mediathek.sqlite

import mediathek.Main
import mediathek.config.Konstanten
import mediathek.controller.history.SeenHistoryStore
import mediathek.mainwindow.MediathekGui
import org.apache.logging.log4j.LogManager
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.awt.Component
import java.nio.file.Files
import java.nio.file.Path
import java.sql.SQLException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

internal object SeenHistoryCorruptionHandler {
    private val logger = LogManager.getLogger()
    private val warningShown = AtomicBoolean(false)

    fun openStoreOrThrowCorrupt(
        dbPath: Path,
        openStore: (Path) -> SeenHistoryStore,
    ): SeenHistoryStore {
        try {
            return openStore(dbPath)
        } catch (ex: Exception) {
            if (!isCorruptionCandidate(ex)) {
                throw ex
            }
            logger.warn("Seen history database is corrupt: {}", dbPath, ex)
            throw CorruptSeenHistoryDatabaseException(dbPath, ex)
        }
    }

    fun resolveCorruption(
        dbPath: Path,
        openStore: (Path) -> SeenHistoryStore,
    ): SeenHistoryStore {
        hideSplashScreen()
        showCorruptionWarning(dbPath)
        return openTemporaryStore(openStore)
    }

    private fun showCorruptionWarning(dbPath: Path) {
        if (!warningShown.compareAndSet(false, true)) {
            return
        }
        showMessage(
            currentOwner(),
            "<html>Die History-Datenbank ist beschädigt:<br/>$dbPath<br/><br/>" +
                "Für diese Sitzung wird eine leere temporäre History-Datenbank verwendet.<br/>" +
                "Reparieren Sie die Datenbank mittels dem Menüpunkt:<br/>" +
                "<i>Hilfe/Hilfsmittel/History-Datenbank wiederherstellen...</i><br/><br/>" +
                    "<b>Alle gesehenen Filme gehen ab jetzt verloren!</b></html>",
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun openTemporaryStore(openStore: (Path) -> SeenHistoryStore): SeenHistoryStore {
        val tempDb = Files.createTempFile("mediathek-history-session-", ".db")
        tempDb.toFile().deleteOnExit()
        return openStore(tempDb)
    }

    internal fun isCorruptionCandidate(ex: Throwable): Boolean {
        return generateSequence(ex) { it.cause }
            .any { throwable ->
                when (throwable) {
                    is SQLiteException -> throwable.isLikelyCorruption()

                    is SQLException -> throwable.messageIndicatesCorruption()

                    else -> false
                }
            }
    }

    private fun SQLiteException.isLikelyCorruption(): Boolean {
        if (resultCode == SQLiteErrorCode.SQLITE_CORRUPT ||
            resultCode == SQLiteErrorCode.SQLITE_CORRUPT_INDEX ||
            resultCode == SQLiteErrorCode.SQLITE_CORRUPT_SEQUENCE ||
            resultCode == SQLiteErrorCode.SQLITE_CORRUPT_VTAB ||
            resultCode == SQLiteErrorCode.SQLITE_NOTADB
        ) {
            return true
        }

        if (messageIndicatesCorruption()) {
            return true
        }

        return !messageIndicatesOperationalFailure()
    }

    private fun SQLException.messageIndicatesCorruption(): Boolean {
        val normalizedMessage = message?.lowercase(Locale.ROOT) ?: return false
        return normalizedMessage.contains("database disk image is malformed") ||
            normalizedMessage.contains("file is not a database") ||
            normalizedMessage.contains("not a database")
    }

    private fun SQLException.messageIndicatesOperationalFailure(): Boolean {
        val normalizedMessage = message?.lowercase(Locale.ROOT) ?: return false
        return normalizedMessage.contains("database is locked") ||
            normalizedMessage.contains("database table is locked") ||
            normalizedMessage.contains("busy") ||
            normalizedMessage.contains("unable to open database file") ||
            normalizedMessage.contains("cannot open database") ||
            normalizedMessage.contains("access is denied") ||
            normalizedMessage.contains("read-only database") ||
            normalizedMessage.contains("readonly database")
    }

    private fun currentOwner(): Component? = runCatching { MediathekGui.ui() }.getOrNull()

    private fun showMessage(owner: Component?, message: String, messageType: Int) {
        invokeOnEdt {
            JOptionPane.showMessageDialog(owner, message, Konstanten.PROGRAMMNAME, messageType)
        }
    }

    private fun hideSplashScreen() {
        invokeOnEdt {
            Main.splashScreen.ifPresent { it.isVisible = false }
        }
    }

    private fun <T> invokeOnEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) {
            return block()
        }

        val resultBox = arrayOfNulls<Any>(1)
        val failureBox = arrayOfNulls<Throwable>(1)
        SwingUtilities.invokeAndWait {
            try {
                resultBox[0] = block()
            } catch (ex: Throwable) {
                failureBox[0] = ex
            }
        }
        failureBox[0]?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return resultBox[0] as T
    }

    class CorruptSeenHistoryDatabaseException(
        val dbPath: Path,
        cause: Throwable
    ) : IllegalStateException("Seen history database is corrupt: $dbPath", cause)
}
