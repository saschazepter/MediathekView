package mediathek.daten;

import mediathek.config.Daten;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatenPsetTest {

    @Test
    void parsingAbspielenFlagDoesNotResetGlobalPlaybackSelection() {
        final ListePset listePset = Daten.getInstance().getListePset();
        final ListePset originalState = new ListePset();
        originalState.addAll(listePset);
        try {
            listePset.clear();

            final DatenPset active = new DatenPset("active");
            final DatenPset other = new DatenPset("other");
            listePset.add(active);
            listePset.add(other);
            listePset.activateAsPlayer(active);

            final DatenPset temporary = new DatenPset();
            temporary.set(DatenPset.PROGRAMMSET_IST_ABSPIELEN, Boolean.TRUE.toString());

            assertTrue(active.istAbspielen());
            assertFalse(other.istAbspielen());
            assertTrue(temporary.istAbspielen());
        } finally {
            listePset.clear();
            listePset.addAll(originalState);
        }
    }

    @Test
    void directAddKeepsOnlyOnePlaybackSelection() {
        final ListePset listePset = new ListePset();

        final DatenPset first = new DatenPset("first");
        first.setAbspielen(true);
        listePset.add(first);

        final DatenPset second = new DatenPset("second");
        second.setAbspielen(true);
        listePset.add(second);

        assertFalse(first.istAbspielen());
        assertTrue(second.istAbspielen());
        assertEquals(second, listePset.getPsetAbspielen());
    }

    @Test
    void addAllKeepsOnlyLastPlaybackSelectionFromIncomingCollection() {
        final ListePset listePset = new ListePset();

        final DatenPset existing = new DatenPset("existing");
        existing.setAbspielen(true);
        listePset.add(existing);

        final DatenPset firstIncoming = new DatenPset("firstIncoming");
        firstIncoming.setAbspielen(true);
        final DatenPset secondIncoming = new DatenPset("secondIncoming");
        secondIncoming.setAbspielen(true);

        listePset.addAll(List.of(firstIncoming, secondIncoming));

        assertFalse(existing.istAbspielen());
        assertFalse(firstIncoming.istAbspielen());
        assertTrue(secondIncoming.istAbspielen());
        assertEquals(secondIncoming, listePset.getPsetAbspielen());
    }
}
