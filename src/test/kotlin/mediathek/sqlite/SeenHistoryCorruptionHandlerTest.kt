package mediathek.sqlite

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.nio.file.Path
import java.sql.SQLException

class SeenHistoryCorruptionHandlerTest {
    @Test
    fun wrapsSqlExceptionWhenMessageClearlyIndicatesCorruption() {
        assertThrows(SeenHistoryCorruptionHandler.CorruptSeenHistoryDatabaseException::class.java) {
            SeenHistoryCorruptionHandler.openStoreOrThrowCorrupt(Path.of("history.db")) {
                throw SQLException("The database disk image is malformed")
            }
        }
    }

    @Test
    fun doesNotWrapUnrelatedSqlException() {
        assertThrows(SQLException::class.java) {
            SeenHistoryCorruptionHandler.openStoreOrThrowCorrupt(Path.of("history.db")) {
                throw SQLException("database is locked")
            }
        }
    }

    @Test
    fun doesNotWrapOperationalSqliteFailure() {
        assertThrows(SQLiteException::class.java) {
            SeenHistoryCorruptionHandler.openStoreOrThrowCorrupt(Path.of("history.db")) {
                throw SQLiteException("database is locked", SQLiteErrorCode.SQLITE_BUSY)
            }
        }
    }
}
