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

package mediathek.gui.tabs.tab_film;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkStartupReloadCoordinatorTest {

    @Test
    void nonBookmarkedFilterReloadsImmediatelyAfterFilmListLoad() {
        var coordinator = new BookmarkStartupReloadCoordinator();

        coordinator.onFilmListLoadingStarted();

        assertTrue(coordinator.onFilmListLoaded(false));
        assertFalse(coordinator.onBookmarkRefreshCompleted(false));
    }

    @Test
    void bookmarkedOnlyFilterWaitsForBookmarkRefreshBeforeReload() {
        var coordinator = new BookmarkStartupReloadCoordinator();

        coordinator.onFilmListLoadingStarted();

        assertFalse(coordinator.onFilmListLoaded(true));
        assertTrue(coordinator.onBookmarkRefreshCompleted(true));
    }

    @Test
    void unrelatedBookmarkRefreshDoesNotReloadWithoutPendingStartupWait() {
        var coordinator = new BookmarkStartupReloadCoordinator();

        assertFalse(coordinator.onBookmarkRefreshCompleted(true));
        coordinator.onFilmListLoadingStarted();
        assertFalse(coordinator.onBookmarkRefreshCompleted(true));
    }

    @Test
    void bookmarkedOnlyStartupReloadFiresOnlyOncePerLoadCycle() {
        var coordinator = new BookmarkStartupReloadCoordinator();

        coordinator.onFilmListLoadingStarted();

        assertFalse(coordinator.onFilmListLoaded(true));
        assertTrue(coordinator.onBookmarkRefreshCompleted(true));
        assertFalse(coordinator.onBookmarkRefreshCompleted(true));
    }
}
