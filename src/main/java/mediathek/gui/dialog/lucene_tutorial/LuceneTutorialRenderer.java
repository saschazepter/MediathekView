/*
 * Copyright (c) 2026 derreisende77.
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
package mediathek.gui.dialog.lucene_tutorial;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LuceneTutorialRenderer {
    private LuceneTutorialRenderer() {
    }

    public static @NotNull String renderMarkdown(@NotNull String markdown) {
        var extensions = List.of(TablesExtension.create());
        var parser = Parser.builder().extensions(extensions).build();
        var renderer = HtmlRenderer.builder().extensions(extensions).build();
        var body = renderer.render(parser.parse(markdown));
        return """
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                body {
                  font-family: Arial, Helvetica, sans-serif;
                  font-size: 12pt;
                  margin: 14px;
                  line-height: 1.4;
                }
                h1, h2, h3 {
                  margin-top: 18px;
                  margin-bottom: 8px;
                }
                p, ul, ol, table, pre {
                  margin-top: 8px;
                  margin-bottom: 8px;
                }
                table {
                  border-collapse: collapse;
                }
                th, td {
                  border: 1px solid #b8b8b8;
                  padding: 6px 8px;
                  text-align: left;
                  vertical-align: top;
                }
                code, pre {
                  font-family: Monospaced;
                }
                pre {
                  background: #f5f5f5;
                  border: 1px solid #d8d8d8;
                  padding: 8px;
                }
                a {
                  color: #0b57d0;
                  text-decoration: underline;
                }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(body);
    }
}
