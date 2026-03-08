/*
 * Copyright (c) 2025 derreisende77.
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

package mediathek.daten.blacklist;

import mediathek.config.MVConfig;
import mediathek.daten.DatenFilm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplyBlacklistFilterPredicateTest {
    @BeforeEach
    void setUp() {
        MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST, Boolean.FALSE.toString());
    }

    @Test
    void splitTerms() {
        final String input = "a,b,c,d";
        var filter = new ApplyBlacklistFilterPredicate(new ListeBlacklist());
        var result = filter.splitTerms(input);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("d", result[3]);
    }

    @Test
    void shouldRejectFilmWhenSenderSpecificRuleMatches() {
        final var blacklist = createBlacklist(rule("ndr", "hamburg journal"));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertFalse(filter.test(film("ndr", "hamburg journal", "folge 1")));
    }

    @Test
    void shouldKeepFilmWhenSenderSpecificRuleHasDifferentSender() {
        final var blacklist = createBlacklist(rule("ndr", "hamburg journal"));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertTrue(filter.test(film("ard", "hamburg journal", "folge 1")));
    }

    @Test
    void shouldRejectFilmWhenGlobalThemaRuleMatchesWithoutSender() {
        final var blacklist = createBlacklist(rule("", "sachsenspiegel"));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertFalse(filter.test(film("mdr", "sachsenspiegel", "abend")));
    }

    @Test
    void shouldRejectFilmWithMixedCaseFilmDataAgainstNormalizedRule() {
        final var blacklist = createBlacklist(rule("ard", "tagesschau"));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertFalse(filter.test(film("ARD", "Tagesschau", "20 Uhr")));
    }

    @Test
    void shouldRejectFilmWhenAnyOfMultipleRulesMatches() {
        final var blacklist = createBlacklist(
                rule("ndr", "hamburg journal"),
                rule("zdf", "heute-show"),
                rule("", "tierärztin dr. mertens"));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertFalse(filter.test(film("ZDF", "Heute-Show", "sendung")));
        assertFalse(filter.test(film("ard", "tierärztin dr. mertens", "episode")));
        assertTrue(filter.test(film("zdf", "aspekte", "magazin")));
    }

    @Test
    void shouldPreserveRegexCaseForTitleRules() {
        final var blacklist = createBlacklist(new BlacklistRule("", "", "#:(?-i:ABC)", ""));
        final var filter = new ApplyBlacklistFilterPredicate(blacklist);

        assertFalse(filter.test(film("ard", "tagesschau", "ABC")));
        assertTrue(filter.test(film("ard", "tagesschau", "abc")));
    }

    private ListeBlacklist createBlacklist(BlacklistRule... rules) {
        final var blacklist = new ListeBlacklist();
        for (BlacklistRule rule : rules) {
            rule.checkPatterns();
            blacklist.addWithoutNotification(rule);
        }
        return blacklist;
    }

    private BlacklistRule rule(String sender, String thema) {
        return new BlacklistRule(sender, thema, "", "");
    }

    private DatenFilm film(String sender, String thema, String title) {
        final var film = new DatenFilm();
        film.setSender(sender);
        film.setThema(thema);
        film.setTitle(title);
        return film;
    }
}
