package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.util.FontUtilities;
import rars.util.FontWeight;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class FontSettings extends SettingsBase {
    private static final Logger LOGGER = LogManager.getLogger(FontSettings.class);

    // region Preferences keys
    private static final String FONT_PREFIX = "Font";
    private static final String SIZE = "Size";
    private static final String FAMILY = "Family";
    private static final String WEIGHT = "Weight";
    private static final String LIGATURES = "Ligatures";
    // endregion Preferences keys

    public static @NotNull FontSettings FONT_SETTINGS = new FontSettings(SETTINGS_PREFERENCES);

    private final @NotNull Preferences preferences;
    public @NotNull FontWeight fontWeight;
    public boolean isLigaturized;
    private int fontSize;
    private @NotNull String fontFamily;

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
        if (isLigaturized) {
            attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        }
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
        if (fontSize < FontUtilities.MIN_SIZE || fontSize > FontUtilities.MAX_SIZE) {
            LOGGER.error("Attempted to set invalid font size: {}", fontSize);
            throw new IllegalArgumentException(
                "Font size must be between %d and %d. Provided: %d".formatted(
                    FontUtilities.MIN_SIZE,
                    FontUtilities.MAX_SIZE,
                    fontSize
                ));
        }
        this.fontSize = fontSize;
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
        this.onChangeDispatcher.dispatch(null);
    }

    private void loadSettingsFromPreferences() {
        this.fontFamily = preferences.get(FONT_PREFIX + FAMILY, "Monospaced");
        this.fontSize = preferences.getInt(FONT_PREFIX + SIZE, FontUtilities.DEFAULT_SIZE);
        this.fontWeight = FontWeight.valueOf(preferences.get(FONT_PREFIX + WEIGHT, FontWeight.REGULAR.name()));
        this.isLigaturized = preferences.getBoolean(FONT_PREFIX + LIGATURES, false);
    }
}
