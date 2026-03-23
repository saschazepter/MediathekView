package mediathek.daten;

import mediathek.config.Daten;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatenPsetTest {

    @Test
    void parsingAbspielenFlagDoesNotResetGlobalPlaybackSelection() {
        final ListePset originalListePset = Daten.listePset;
        try {
            Daten.listePset = new ListePset();

            final DatenPset active = new DatenPset("active");
            final DatenPset other = new DatenPset("other");
            Daten.listePset.add(active);
            Daten.listePset.add(other);
            Daten.listePset.activateAsPlayer(active);

            final DatenPset temporary = new DatenPset();
            temporary.set(DatenPset.PROGRAMMSET_IST_ABSPIELEN, Boolean.TRUE.toString());

            assertTrue(active.istAbspielen());
            assertFalse(other.istAbspielen());
            assertTrue(temporary.istAbspielen());
        } finally {
            Daten.listePset = originalListePset;
        }
    }
}
