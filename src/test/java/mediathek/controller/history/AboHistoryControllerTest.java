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

package mediathek.controller.history;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class AboHistoryControllerTest {
    private Path tempDirectory;
    private Path legacyFile;
    private Path databaseFile;

    @BeforeEach
    void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("abo-history-controller-test");
        legacyFile = tempDirectory.resolve("downloadAbos.txt");
        databaseFile = tempDirectory.resolve("abo-history.db");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDirectory.resolve("downloadAbos.txt.migrated"));
        Files.deleteIfExists(legacyFile);
        Files.deleteIfExists(databaseFile);
        Files.deleteIfExists(tempDirectory.resolve("abo-history.db-shm"));
        Files.deleteIfExists(tempDirectory.resolve("abo-history.db-wal"));
        Files.deleteIfExists(tempDirectory);
    }

    @Test
    void migratesLegacyTextFileOnStartup() throws IOException {
        Files.writeString(legacyFile, """
                02.11.2020 |#| Thema A |#| Titel A  |###|  https://example.org/a.mp4
                02.11.2020 |#| Thema B |#| Titel B  |###|  https://example.org/b.mp4
                02.11.2020 |#| Thema B |#| Titel B  |###|  https://example.org/b.mp4
                invalid line
                02.11.2020 |#| Thema C |#| Titel C  |###|  rtmp://example.org/c
                """, StandardCharsets.UTF_8);

        var controller = new AboHistoryController(legacyFile, databaseFile);

        assertTrue(Files.exists(databaseFile));
        assertFalse(Files.exists(legacyFile));
        assertTrue(Files.exists(tempDirectory.resolve("downloadAbos.txt.migrated")));

        assertEquals(2, controller.getDataList().size());
        assertTrue(controller.urlExists("https://example.org/a.mp4"));
        assertTrue(controller.urlExists("https://example.org/b.mp4"));
        assertFalse(controller.urlExists("https://example.org/c.mp4"));
    }

    @Test
    void persistsEntriesInSqliteAcrossControllerInstances() {
        var controller = new AboHistoryController(legacyFile, databaseFile);
        controller.add(new MVUsedUrl("02.11.2020", "Thema A", "Titel A", "https://example.org/a.mp4"));
        controller.add(new MVUsedUrl("02.11.2020", "Thema A", "Titel A", "https://example.org/a.mp4"));
        controller.add(new MVUsedUrl("02.11.2020", "Thema B", "Titel B", "https://example.org/b.mp4"));

        assertEquals(2, controller.getDataList().size());
        assertTrue(controller.urlExists("https://example.org/a.mp4"));

        var reloadedController = new AboHistoryController(legacyFile, databaseFile);
        assertEquals(2, reloadedController.getDataList().size());

        reloadedController.removeUrl("https://example.org/a.mp4");
        assertFalse(reloadedController.urlExists("https://example.org/a.mp4"));
        assertEquals(1, reloadedController.getDataList().size());

        reloadedController.removeAll();
        assertTrue(reloadedController.getDataList().isEmpty());
    }

    @Test
    void removesMultipleEntriesAtOnce() {
        var controller = new AboHistoryController(legacyFile, databaseFile);
        controller.add(new MVUsedUrl("02.11.2020", "Thema A", "Titel A", "https://example.org/a.mp4"));
        controller.add(new MVUsedUrl("02.11.2020", "Thema B", "Titel B", "https://example.org/b.mp4"));
        controller.add(new MVUsedUrl("02.11.2020", "Thema C", "Titel C", "https://example.org/c.mp4"));

        var removedCount = controller.removeUrls(java.util.List.of(
                "https://example.org/a.mp4",
                "https://example.org/c.mp4",
                "https://example.org/a.mp4"));

        assertEquals(2, removedCount);
        assertFalse(controller.urlExists("https://example.org/a.mp4"));
        assertTrue(controller.urlExists("https://example.org/b.mp4"));
        assertFalse(controller.urlExists("https://example.org/c.mp4"));
        assertEquals(1, controller.getDataList().size());
    }

    @Test
    void bootstrapsSchemaVersionForFutureMigrations() throws Exception {
        new AboHistoryController(legacyFile, databaseFile);

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("PRAGMA user_version")) {
            assertTrue(resultSet.next());
            assertEquals(AboHistoryDatabaseBootstrapper.CURRENT_SCHEMA_VERSION, resultSet.getInt(1));
        }
    }
}
