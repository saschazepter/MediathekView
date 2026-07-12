package mediathek.gui.tabs.tab_film.filter

import mediathek.gui.tabs.tab_film.filter.SwingFilterDialogTestFixture.assumeUiAvailable
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialogTestFixture.createDialogSetup
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialogTestFixture.onEdt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SwingFilterDialogLifecycleTest {
    @Test
    fun `disposing dialog closes its sender event list exactly once safely`() {
        assumeUiAvailable()
        val setup = createDialogSetup()

        onEdt {
            setup.dialog.dispose()
            setup.dialog.dispose()
            setup.model.close()
        }

        assertEquals(1, setup.senderListDisposeCalls())
    }
}
