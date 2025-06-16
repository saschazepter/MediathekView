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

import java.util.*;
import java.util.regex.Pattern;

public class TitleParserManager {
    private final Map<String, RuleBasedTitleParser> parsers = new HashMap<>();

    /**
     * Register a sender with its regex patterns.
     * @param sender the key (e.g. "3Sat")
     * @param patterns varargs of regex strings with named groups 'season' and 'episode'
     */
    public void register(String sender, String... patterns) {
        List<Pattern> compiled = new ArrayList<>();
        for (String regex : patterns) {
            // enable named groups, case-insensitive by default
            compiled.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
        parsers.put(sender, new RuleBasedTitleParser(compiled));
    }

    /**
     * Parse a title for a given sender.
     */
    public Optional<SeasonEpisode> parse(String sender, String title) {
        RuleBasedTitleParser parser = parsers.get(sender);
        if (parser == null) {
            return Optional.empty();
        }
        return parser.parse(title);
    }
}