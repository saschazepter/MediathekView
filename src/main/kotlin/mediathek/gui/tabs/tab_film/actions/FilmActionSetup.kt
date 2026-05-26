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

package mediathek.gui.tabs.tab_film.actions

import mediathek.gui.actions.ManageBookmarkAction
import mediathek.gui.actions.PlayFilmAction

class FilmActionSetup(
    private val playFilmAction: PlayFilmAction,
    private val saveFilmAction: SaveFilmAction,
    private val copyHqUrlToClipboardAction: CopyUrlToClipboardAction,
    private val copyNormalUrlToClipboardAction: CopyUrlToClipboardAction,
    private val toggleFilterDialogVisibilityAction: ToggleFilterDialogVisibilityAction,
    private val bookmarkAddFilmAction: BookmarkAddFilmAction,
    private val bookmarkRemoveFilmAction: BookmarkRemoveFilmAction,
    private val manageBookmarkAction: ManageBookmarkAction,
    private val filmUiActions: FilmUiActions,
) {
    fun playFilmAction(): PlayFilmAction = playFilmAction
    fun saveFilmAction(): SaveFilmAction = saveFilmAction
    fun copyHqUrlToClipboardAction(): CopyUrlToClipboardAction = copyHqUrlToClipboardAction
    fun copyNormalUrlToClipboardAction(): CopyUrlToClipboardAction = copyNormalUrlToClipboardAction
    fun toggleFilterDialogVisibilityAction(): ToggleFilterDialogVisibilityAction = toggleFilterDialogVisibilityAction
    fun bookmarkAddFilmAction(): BookmarkAddFilmAction = bookmarkAddFilmAction
    fun bookmarkRemoveFilmAction(): BookmarkRemoveFilmAction = bookmarkRemoveFilmAction
    fun manageBookmarkAction(): ManageBookmarkAction = manageBookmarkAction
    fun filmUiActions(): FilmUiActions = filmUiActions
}
