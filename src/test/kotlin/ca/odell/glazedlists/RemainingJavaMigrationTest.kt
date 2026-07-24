package ca.odell.glazedlists

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RemainingJavaMigrationTest {
    @Test
    fun manifestMatchesRemainingGlazedListsJavaSources() {
        val actual = remainingJavaSources()

        assertEquals(expectedRemainingJavaSources.sorted(), actual)
        assertEquals(0, expectedRemainingJavaSources.size)
        assertEquals(0, actual.size)
    }

    @Test
    fun noGlazedListsJavaSourceExistsOutsideTheManifest() {
        val actual = remainingJavaSources().toSet()

        val expectedSources = expectedRemainingJavaSources.toSet()
        val unexpectedSources = actual - expectedSources
        assertTrue(unexpectedSources.isEmpty(), "Unexpected Glazed Lists Java sources: $unexpectedSources")
    }

    private fun repoRelativePath(path: Path): String = path.toString().replace(File.separatorChar, '/')

    private fun remainingJavaSources(): List<String> {
        if (Files.notExists(glazedListsJavaRoot)) return emptyList()
        return Files.walk(glazedListsJavaRoot).use { paths ->
            paths
                .filter { it.name.endsWith(".java") }
                .map { repoRelativePath(it) }
                .sorted()
                .toList()
        }
    }

    private companion object {
        private val glazedListsJavaRoot = Path.of("src/main/java/ca/odell/glazedlists")

        private val expectedRemainingJavaSources = emptyList<String>()
    }
}
