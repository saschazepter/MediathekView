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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TitleParser {
    // Patterns to try, in order
    private static final Pattern[] PATTERNS = new Pattern[]{
            // e.g. "… (S2024/E04)"
            Pattern.compile("(?i).*?S(\\d{1,4})\\s*/\\s*E(\\d{1,3}).*"),
            // e.g. "… S01E02 …"
            Pattern.compile("(?i).*?S(\\d{1,2})E(\\d{1,2}).*"),
            // e.g. "… Season 1 Episode 2 …"
            Pattern.compile("(?i).*?Season[\\s\\.:-]*(\\d{1,2})[\\s\\.:-]*Episode[\\s\\.:-]*(\\d{1,2}).*"),
            // e.g. "… Season 1 Ep 2 …"
            Pattern.compile("(?i).*?Season[\\s\\.:-]*(\\d{1,2})[\\s\\.:-]*Ep[\\s\\.:-]*(\\d{1,2}).*"),
            // e.g. "… Folge 96 …" (episode only; season left null)
            Pattern.compile("(?i).*?Folge[\\s\\.:-]*(\\d{1,3}).*")
    };

    public static Map<String, SeasonEpisode> parseListConcurrent(List<String> lines) throws IOException {
        return lines
                .parallelStream()                                      // process in parallel
                .map(line -> new AbstractMap.SimpleEntry<>(
                        line,
                        parseSeasonEpisode(line).orElse(null)
                ))
                .filter(e -> e.getValue() != null)               // drop non-matches
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Attempts to extract season and episode numbers from a title.
     *
     * @param title the full title line
     * @return an Optional containing SeasonEpisode if parsing succeeded, or empty otherwise
     */
    public static Optional<SeasonEpisode> parseSeasonEpisode(String title) {
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(title);
            if (m.find()) {
                try {
                    if (p.pattern().toLowerCase().contains("folge")) {
                        // German “Folge” gives only episode
                        int episode = Integer.parseInt(m.group(1));
                        return Optional.of(new SeasonEpisode(null, episode));
                    }
                    else {
                        int season = Integer.parseInt(m.group(1));
                        int episode = Integer.parseInt(m.group(2));
                        return Optional.of(new SeasonEpisode(season, episode));
                    }
                }
                catch (NumberFormatException e) {
                    // skip and try next
                }
            }
        }
        return Optional.empty();
    }

    // Example usage
    public static void main(String[] args) {
        String[] titles = {
                "\"… (S2024/E04)\"",
                "\"My Show S03E07: The Adventure\"",
                "\"Documentary – Season 2 Episode 5\"",
                "\"Talkrunde: Folge 96\"",
                "\"No episode info here\""
        };
        for (var t : titles) {
            System.out.printf("%s -> %s%n",
                    t,
                    parseSeasonEpisode(t)
                            .map(se -> "season=" + se.season() + ", episode=" + se.episode())
                            .orElse("no match")
            );
        }
    }
}
