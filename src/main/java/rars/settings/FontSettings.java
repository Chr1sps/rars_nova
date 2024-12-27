package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.util.FontWeight;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static rars.settings.Settings.SETTINGS_PREFERENCES;

public final class FontSettings extends ListenableBase<FontSettings> {
    private static final Logger LOGGER = LogManager.getLogger();

    // region Preferences keys
    private static final String FONT_PREFIX = "Font";
    private static final String SIZE = "Size";
    private static final String FAMILY = "Family";
    private static final String WEIGHT = "Weight";
    private static final String LIGATURES = "Ligatures";
    // endregion Preferences keys

    public static @NotNull FontSettings FONT_SETTINGS = new FontSettings(SETTINGS_PREFERENCES);

    private final @NotNull Preferences preferences;
    private int fontSize;
    private @NotNull String fontFamily;
    private @NotNull FontWeight fontWeight;
    private boolean isLigaturized;

    // region Getters and setters

    public FontSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        loadSettingsFromPreferences();
    }

    public @NotNull Font getCurrentFont() {
        final var attributes = new HashMap<TextAttribute, Object>();
        attributes.put(TextAttribute.FAMILY, fontFamily);
        attributes.put(TextAttribute.SIZE, fontSize);
        attributes.put(TextAttribute.WEIGHT, fontWeight.weight);
        if (isLigaturized)
            attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        return new Font(attributes);
    }

    public @NotNull String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(final @NotNull String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(final int fontSize) {
        this.fontSize = fontSize;
    }

    public @NotNull FontWeight getFontWeight() {
        return fontWeight;
    }

    public void setFontWeight(final @NotNull FontWeight fontWeight) {
        this.fontWeight = fontWeight;
    }

    public boolean isLigaturized() {
        return isLigaturized;
    }

    public void setLigaturized(final boolean isLigaturized) {
        this.isLigaturized = isLigaturized;
    }

    // endregion Getters and setters

    public void saveSettingsToPreferences() {
        preferences.put(FONT_PREFIX + FAMILY, fontFamily);
        preferences.putInt(FONT_PREFIX + SIZE, fontSize);
        preferences.putBoolean(FONT_PREFIX + LIGATURES, isLigaturized);
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
        submit();
    }

    private void loadSettingsFromPreferences() {
        this.fontFamily = preferences.get(FONT_PREFIX + FAMILY, "Monospaced");
        this.fontSize = preferences.getInt(FONT_PREFIX + SIZE, 12);
        this.fontWeight = FontWeight.valueOf(preferences.get(FONT_PREFIX + WEIGHT, FontWeight.REGULAR.name()));
        this.isLigaturized = preferences.getBoolean(FONT_PREFIX + LIGATURES, false);
    }
}
