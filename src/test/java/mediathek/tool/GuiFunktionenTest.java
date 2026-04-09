package mediathek.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GuiFunktionenTest {

    @Test
    void getFilmListUpdateType() {
        var res = GuiFunktionen.getFilmListUpdateType();
        Assertions.assertSame(FilmListUpdateType.AUTOMATIC, res);
    }

    @Test
    void getSuffixFromUrl() {
        final var testStr = "https://ios-ondemand.swr.de/i/swr-fernsehen/bw-extra/20130202/601676.,m,s,l,.mp4.csmil/index_2_av.m3u8?e=b471643725c47acd";
        final var expected = "m3u8";
        var res = GuiFunktionen.getSuffixFromUrl(testStr);

        Assertions.assertEquals(expected, res);
    }

    @Test
    void getFileNameWithoutExtension_web() {
        final var testStr = "https://ios-ondemand.swr.de/i/swr-fernsehen/bw-extra/20130202/601676.,m,s,l,.mp4.csmil/index_2_av.m3u8?e=b471643725c47acd";
        final var expected = "https://ios-ondemand.swr.de/i/swr-fernsehen/bw-extra/20130202/601676.,m,s,l,.mp4.csmil/index_2_av";
        var res = GuiFunktionen.getFileNameWithoutExtension(testStr);

        Assertions.assertEquals(expected, res);
    }

    @Test
    void getFileNameWithoutExtension_file() {
        final var testStr = "/Users/derreisende/file1.mp4";
        final var expected = "/Users/derreisende/file1";
        var res = GuiFunktionen.getFileNameWithoutExtension(testStr);

        Assertions.assertEquals(expected, res);
    }

    @Test
    void getFileNameWithoutExtension_fileWithQuestionMark() {
        final var testStr = "/Users/derreisende/Downloads/mediathek/Die Nordreportage/Die Nordreportage-Wie geht das? Fertigung eines Windrades-0143177029.mp4";
        final var expected = "/Users/derreisende/Downloads/mediathek/Die Nordreportage/Die Nordreportage-Wie geht das? Fertigung eines Windrades-0143177029";
        var res = GuiFunktionen.getFileNameWithoutExtension(testStr);

        Assertions.assertEquals(expected, res);
    }

    @Test
    void concatPaths() {
        String sep = java.io.File.separator;
        Assertions.assertEquals("", GuiFunktionen.concatPaths(null, null));
        Assertions.assertEquals("", GuiFunktionen.concatPaths(null, "b"));
        Assertions.assertEquals("", GuiFunktionen.concatPaths("a", null));
        Assertions.assertEquals("ab", GuiFunktionen.concatPaths("", "ab"));
        Assertions.assertEquals("ab", GuiFunktionen.concatPaths("ab", ""));

        Assertions.assertEquals("a" + sep + "b", GuiFunktionen.concatPaths("a", "b"));
        Assertions.assertEquals("a" + sep + "b", GuiFunktionen.concatPaths("a" + sep, "b"));
        Assertions.assertEquals("a" + sep + "b", GuiFunktionen.concatPaths("a", sep + "b"));
        Assertions.assertEquals("a" + sep + "b", GuiFunktionen.concatPaths("a" + sep, sep + "b"));

        // Multiple separators at junction
        Assertions.assertEquals("a" + sep + "b", GuiFunktionen.concatPaths("a" + sep + sep, "b"));

        // Windows UNC paths (simulated)
        if (sep.equals("\\")) {
            Assertions.assertEquals("\\\\server\\share\\file", GuiFunktionen.concatPaths("\\\\server\\share", "file"));
            Assertions.assertEquals("\\\\server\\share\\file", GuiFunktionen.concatPaths("\\\\server\\share\\", "file"));
        } else {
            Assertions.assertEquals("//server/share/file", GuiFunktionen.concatPaths("//server/share", "file"));
            Assertions.assertEquals("//server/share/file", GuiFunktionen.concatPaths("//server/share/", "file"));
        }
    }
}
