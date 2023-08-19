/*
 * Created by JFormDesigner on Sat Aug 19 13:40:47 CEST 2023
 */

package mediathek.gui.tabs.cellrenderer;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import mediathek.daten.Country;
import mediathek.daten.DatenFilm;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.CompoundIcon;
import mediathek.tool.SVGIconUtilities;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author christianfranzke
 */
public class TitleCell extends JPanel {
    private static final EnumSet<Country> euCountryList = EnumSet.of(Country.DE, Country.AT, Country.FR);
    protected final FlatSVGIcon lockedIcon;
    protected final FlatSVGIcon lockedIconSelected;
    private final FlatSVGIcon highQualityIcon;
    private final FlatSVGIcon highQualityIconSelected;
    private final FlatSVGIcon audioDescription;
    private final FlatSVGIcon audioDescriptionSelected;
    private final FlatSVGIcon subtitleIcon;
    private final FlatSVGIcon subtitleIconSelected;
    private final FlatSVGIcon liveStreamIcon;
    private final FlatSVGIcon liveStreamIconSelected;
    protected FlatSVGIcon.ColorFilter whiteColorFilter = new FlatSVGIcon.ColorFilter(color -> Color.WHITE);

    public TitleCell() {
        initComponents();

        title.setText("");
        icons.setText("");

        lockedIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/lock.svg");
        lockedIconSelected = SVGIconUtilities.createSVGIcon("icons/fontawesome/lock.svg");
        lockedIconSelected.setColorFilter(whiteColorFilter);

        highQualityIcon = SVGIconUtilities.createSVGIcon("icons/derreisende77/high-quality.svg");
        highQualityIconSelected = SVGIconUtilities.createSVGIcon("icons/derreisende77/high-quality.svg");
        highQualityIconSelected.setColorFilter(whiteColorFilter);

        audioDescription = SVGIconUtilities.createSVGIcon("icons/fontawesome/audio-description.svg");
        audioDescriptionSelected = SVGIconUtilities.createSVGIcon("icons/fontawesome/audio-description.svg");
        audioDescriptionSelected.setColorFilter(whiteColorFilter);

        subtitleIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/closed-captioning.svg");
        subtitleIconSelected = SVGIconUtilities.createSVGIcon("icons/fontawesome/closed-captioning.svg");
        subtitleIconSelected.setColorFilter(whiteColorFilter);

        liveStreamIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/tower-cell.svg");
        liveStreamIconSelected = SVGIconUtilities.createSVGIcon("icons/fontawesome/tower-cell.svg");
        liveStreamIconSelected.setColorFilter(whiteColorFilter);

    }

    private boolean filmIsCountryUnlocked(@NotNull DatenFilm film) {
        var curLocation = ApplicationConfiguration.getInstance().getGeographicLocation();
        //EU consists of many states therefore we have to extend the country test...
        if (film.countrySet.contains(Country.EU)) {
            return film.countrySet.contains(curLocation) || euCountryList.contains(curLocation);
        }
        else {
            return film.countrySet.contains(curLocation);
        }
    }
    public void setIndicatorIcons(@NotNull DatenFilm datenFilm, @NotNull JTable table, boolean isSelected) {
        List<Icon> iconList = new ArrayList<>();

        if (!datenFilm.countrySet.isEmpty()) {
            if (!filmIsCountryUnlocked(datenFilm)) {
                //locked
                if (isSelected)
                    iconList.add(lockedIconSelected);
                else
                    iconList.add(lockedIcon);
            }
        }

        var tc = table.getColumn("HQ");
        // if HQ column is NOT visible add icon
        if (tc.getWidth() == 0) {
            if (datenFilm.isHighQuality()) {
                if (isSelected)
                    iconList.add(highQualityIconSelected);
                else
                    iconList.add(highQualityIcon);
            }
        }

        if (datenFilm.isAudioVersion()) {
            if (isSelected)
                iconList.add(audioDescriptionSelected);
            else
                iconList.add(audioDescription);
        }

        tc = table.getColumn("UT");
        //if UT column is NOT visible
        if (tc.getWidth() == 0) {
            if (datenFilm.hasSubtitle()) {
                if (isSelected)
                    iconList.add(subtitleIconSelected);
                else
                    iconList.add(subtitleIcon);
            }
        }

        if (datenFilm.isLivestream()) {
            if (isSelected)
                iconList.add(liveStreamIconSelected);
            else
                iconList.add(liveStreamIcon);
        }

        Icon icon;
        if (iconList.size() == 1)
            icon = iconList.get(0);
        else
            icon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 3, iconList.toArray(new Icon[0]));
        icons.setIcon(icon);

        //always clear at the end
        iconList.clear();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        title = new JLabel();
        icons = new JLabel();

        //======== this ========
        setLayout(new MigLayout(
            new LC().fillX().insets("0").hideMode(3), //NON-NLS
            // columns
            new AC()
                .grow().fill().gap()
                .align("right"), //NON-NLS
            // rows
            new AC()
                .grow().align("center"))); //NON-NLS

        //---- title ----
        title.setText("text"); //NON-NLS
        add(title, new CC().cell(0, 0).growY());

        //---- icons ----
        icons.setText("blablabla"); //NON-NLS
        add(icons, new CC().cell(1, 0).alignX("trailing").grow(0, 100)); //NON-NLS
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    JLabel title;
    JLabel icons;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
