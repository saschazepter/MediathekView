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

package mediathek.tool.episodes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TitleParserTest {

    @Test
    void parseSeasonEpisode() {
        String[] titles = {

        };

        var opt = TitleParser.parseSeasonEpisode("\"… (S2024/E04)\"");
        assertTrue(opt.isPresent());
        opt.ifPresent(info -> assertEquals(2024, info.season()));
        opt.ifPresent(info -> assertEquals(4, info.episode()));

        opt = TitleParser.parseSeasonEpisode("\"My Show S03E07: The Adventure\"");
        assertTrue(opt.isPresent());
        opt.ifPresent(info -> assertEquals(3, info.season()));
        opt.ifPresent(info -> assertEquals(7, info.episode()));

        opt = TitleParser.parseSeasonEpisode("\"Documentary – Season 2 Episode 5\"");
        assertTrue(opt.isPresent());
        opt.ifPresent(info -> assertEquals(2, info.season()));
        opt.ifPresent(info -> assertEquals(5, info.episode()));

        opt = TitleParser.parseSeasonEpisode("\"Talkrunde: Folge 96\"");
        assertTrue(opt.isPresent());
        opt.ifPresent(info -> assertNull(info.season()));
        opt.ifPresent(info -> assertEquals(96, info.episode()));

        opt = TitleParser.parseSeasonEpisode("\"No episode info here\"");
        assertFalse(opt.isPresent());
    }
}