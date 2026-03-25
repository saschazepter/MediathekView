package mediathek.tool;

import com.formdev.flatlaf.FlatLaf;

import java.awt.*;

public class MVC {

    private final String text;
    private final String key;
    private final Color lightDefaultColor;
    private final Color darkDefaultColor;
    private Color lightOverrideColor;
    private Color darkOverrideColor;

    public MVC(String key, Color color, String ttext) {
        this(key, color, color, ttext);
    }

    public MVC(String key, Color lightColor, Color darkColor, String ttext) {
        this.key = key;
        text = ttext;
        this.lightDefaultColor = lightColor;
        this.darkDefaultColor = darkColor;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return getColor(FlatLaf.isLafDark());
    }

    public Color getColor(boolean darkMode) {
        if (darkMode) {
            return darkOverrideColor != null ? darkOverrideColor : darkDefaultColor;
        }
        return lightOverrideColor != null ? lightOverrideColor : lightDefaultColor;
    }

    public void set(Color c) {
        lightOverrideColor = c;
        darkOverrideColor = c;
    }

    public void setColor(boolean darkMode, Color c) {
        if (darkMode) {
            darkOverrideColor = c;
        } else {
            lightOverrideColor = c;
        }
    }

    public boolean hasOverride() {
        return lightOverrideColor != null || darkOverrideColor != null;
    }

    public boolean hasOverride(boolean darkMode) {
        return darkMode ? darkOverrideColor != null : lightOverrideColor != null;
    }

    public Color getOverrideColor(boolean darkMode) {
        return darkMode ? darkOverrideColor : lightOverrideColor;
    }

    public void reset() {
        lightOverrideColor = null;
        darkOverrideColor = null;
    }

    public void reset(boolean darkMode) {
        if (darkMode) {
            darkOverrideColor = null;
        } else {
            lightOverrideColor = null;
        }
    }
}
