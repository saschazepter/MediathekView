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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class RuleBasedTitleParser {
    private final List<RulePattern> patterns;

    public RuleBasedTitleParser(List<RulePattern> patterns) {
        this.patterns = patterns;
    }

    /**
     * Try to match each pattern against the title.
     * @return Optional.of(SeasonEpisode) if a pattern matches; otherwise Optional.empty().
     */
    public Optional<SeasonEpisode> parse(String title) {
        final String titleLowercase = title.toLowerCase(Locale.ROOT);
        for (var rulePattern : patterns) {
            if (!rulePattern.matchesGuards(title, titleLowercase)) {
                continue;
            }

            var m = rulePattern.pattern().matcher(title);
            if (m.find()) {
                int season = Integer.parseInt(m.group("season"));
                int episode = Integer.parseInt(m.group("episode"));
                return Optional.of(new SeasonEpisode(season, episode));
            }
        }
        return Optional.empty();
    }

    record RulePattern(Pattern pattern, String[] requiredMarkers) {
        boolean matchesGuards(String title, String titleLowercase) {
            for (String marker : requiredMarkers) {
                if (!containsMarker(title, titleLowercase, marker)) {
                    return false;
                }
            }
            return true;
        }

        private boolean containsMarker(String title, String titleLowercase, String marker) {
            if (marker.equals(marker.toLowerCase(Locale.ROOT))) {
                return titleLowercase.contains(marker);
            }
            return title.contains(marker);
        }
    }
}
