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

package mediathek.tool.cellrenderer;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.ScaledImageIcon;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.sender_icon_cache.MVSenderIconCache;
import mediathek.tool.sender_icon_cache.SenderIconRenderUtil;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for all cell renderer.
 */
public class CellRendererBase extends DefaultTableCellRenderer {
    /**
     * Stores the pre-scaled icon for a specific sender and a specific cell dimension.
     * Will get evicted automatically in order to not store too many useless objects.
     */
    private final SelfEvictingSenderIconCache senderCellIconCache = new SelfEvictingSenderIconCache();

    /**
     * Draws the sender icon in the sender model column.
     *
     * @param sender Name of the sender.
     */
    protected void setSenderIcon(@NotNull String sender, @NotNull Dimension targetDim) {
        // make target dims for icon slightly smaller
        if (SystemUtils.IS_OS_LINUX) {
            targetDim.width -= 4;
            targetDim.height -= 4;
        }

        boolean useLocalSenderIcons = ApplicationConfiguration.getConfiguration()
                .getBoolean(MVSenderIconCache.CONFIG_USE_LOCAL_SENDER_ICONS, false);
        var key = new SenderCacheKey(sender, targetDim, useLocalSenderIcons);
        final AtomicReference<Icon> cachedIcon = new AtomicReference<>();
        cachedIcon.set(senderCellIconCache.getOrDefault(key, null));
        if (cachedIcon.get() == null) {
            MVSenderIconCache.get(sender).ifPresentOrElse(icon -> {
                Icon renderedIcon;
                if (icon instanceof FlatSVGIcon svg) {
                    Icon autoFitted = SenderIconRenderUtil.deriveSvgFittedToOpaqueBounds(svg, targetDim);
                    if (requiresExtraSvgBoost(sender)) {
                        int boostedWidth = Math.max(1, (int) Math.round(autoFitted.getIconWidth() * 1.35d));
                        int boostedHeight = Math.max(1, (int) Math.round(autoFitted.getIconHeight() * 1.35d));
                        renderedIcon = svg.derive(boostedWidth, boostedHeight);
                    } else {
                        renderedIcon = autoFitted;
                    }
                } else {
                    var destDim = SenderIconRenderUtil.calculateFittedDimensionAllowUpscale(
                            new Dimension(icon.getIconWidth(), icon.getIconHeight()),
                            targetDim
                    );
                    renderedIcon = new ScaledImageIcon(icon, destDim.width, destDim.height);
                }
                cachedIcon.set(renderedIcon);
                senderCellIconCache.put(key, cachedIcon.get());
            }, () -> cachedIcon.set(null));
        }

        if (cachedIcon.get() != null) {
            setText("");
            setIcon(cachedIcon.get());
        }
        setVerticalAlignment(SwingConstants.CENTER);
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    private static boolean requiresExtraSvgBoost(@NotNull String sender) {
        String normalized = sender.toLowerCase(Locale.ROOT);
        return normalized.equals("tagesschau24")
                || normalized.equals("radio bremen")
                || normalized.equals("radio bremen tv");
    }

    /**
     * Calculate the dimensions of a table cell for the sender icon.
     *
     * @param table            where it will be displayed.
     * @param row              the used row index.
     * @param columnModelIndex the used column index.
     * @return the calculated dimension of the available table cell.
     */
    protected Dimension getSenderCellDimension(@NotNull JTable table, int row, int columnModelIndex) {
        Dimension targetDim = new Dimension();
        targetDim.height = table.getRowHeight(row);
        targetDim.width = table.getColumnModel().getColumn(columnModelIndex).getWidth();
        targetDim.height -= 4;
        targetDim.width -= 4;
        return targetDim;
    }
}
