package mediathek.sqlite

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
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
}
