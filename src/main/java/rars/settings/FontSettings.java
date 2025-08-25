package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.util.FontWeight;
import rars.util.ListenerDispatcher;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class FontSettings {
    private static final @NotNull Logger LOGGER = LogManager.getLogger(FontSettings.class);

    private static final int MIN_FONT_SIZE = 6;
    private static final int MAX_FONT_SIZE = 72;

    // region Defaults

    private static final @NotNull String DEFAULT_FONT_FAMILY = "Monospaced";
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final @NotNull FontWeight DEFAULT_FONT_WEIGHT = FontWeight.REGULAR;
    private static final boolean DEFAULT_LIGATURES = false;

    // endregion Defaults

    // region Preferences keys

    private static final @NotNull String FONT_PREFIX = "Font";
    private static final @NotNull String SIZE = "Size";
    private static final @NotNull String FAMILY = "Family";
    private static final @NotNull String WEIGHT = "Weight";
    private static final @NotNull String LIGATURES = "Ligatures";

    // endregion Preferences keys

    public final @NotNull ListenerDispatcher<Void>.Hook onChangeListenerHook;
    private final @NotNull ListenerDispatcher<Void> onChangeDispatcher;
    private final @NotNull Preferences preferences;

    private @NotNull FontWeight fontWeight;
    private boolean isLigaturized;
    private int fontSize;
    private @NotNull String fontFamily;

    public FontSettings(final @NotNull Preferences preferences) {
        this.onChangeDispatcher = new ListenerDispatcher<>();
        this.onChangeListenerHook = this.onChangeDispatcher.getHook();
        this.preferences = preferences;
        loadSettingsFromPreferences();
    }

    // region Getters and setters

    public boolean isLigaturized() {
        return isLigaturized;
    }

    public void setLigaturized(final boolean ligaturized) {
        isLigaturized = ligaturized;
    }

    public @NotNull FontWeight getFontWeight() {
        return fontWeight;
    }

    public void setFontWeight(@NotNull final FontWeight fontWeight) {
        this.fontWeight = fontWeight;
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
        if (fontSize < MIN_FONT_SIZE || fontSize > MAX_FONT_SIZE) {
            LOGGER.error("Attempted to set invalid font size: {}", fontSize);
            throw new IllegalArgumentException(
                "Font size must be between %d and %d. Provided: %d".formatted(
                    MIN_FONT_SIZE,
                    MAX_FONT_SIZE,
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
        preferences.put(FONT_PREFIX + WEIGHT, fontWeight.name());
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
        this.fontFamily = preferences.get(FONT_PREFIX + FAMILY, DEFAULT_FONT_FAMILY);
        this.fontSize = preferences.getInt(FONT_PREFIX + SIZE, DEFAULT_FONT_SIZE);
        this.fontWeight = FontWeight.valueOf(preferences.get(FONT_PREFIX + WEIGHT, DEFAULT_FONT_WEIGHT.name()));
        this.isLigaturized = preferences.getBoolean(FONT_PREFIX + LIGATURES, DEFAULT_LIGATURES);
    }
}
