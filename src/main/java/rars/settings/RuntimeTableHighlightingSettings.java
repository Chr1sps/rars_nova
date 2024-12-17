package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.notices.SettingsNotice;
import rars.util.CustomPublisher;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static rars.util.Utils.getColorAsHexString;

public final class RuntimeTableHighlightingSettings extends CustomPublisher<SettingsNotice> {

    private static final Logger LOGGER = LogManager.getLogger();

    // region Preferences keys
    private static final String HIGHLIGHTING_PREFIX = "Highlighting";

    private static final String BACKGROUND = "Background";
    private static final String FOREGROUND = "Foreground";
    private static final String BOLD = "Bold";
    private static final String ITALIC = "Italic";
    private static final String UNDERLINE = "Underline";
    private static final String ENABLED = "Enabled";
    // endregion Preferences keys

    private final @NotNull Preferences preferences;
    private @NotNull TokenStyle textSegmentHighlightingStyle, delaySlotHighlightingStyle;
    private @Nullable TokenStyle dataSegmentHighlightingStyle, registerHighlightingStyle;

    public RuntimeTableHighlightingSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        this.loadSettingsFromPreferences();
    }

    // region Preferences prefix methods
    private static @NotNull String foregroundPrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + FOREGROUND;
    }

    private static @NotNull String backgroundPrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + BACKGROUND;
    }

    private static @NotNull String boldPrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + BOLD;
    }

    private static @NotNull String italicPrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + ITALIC;
    }

    private static @NotNull String underlinePrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + UNDERLINE;
    }

    private static @NotNull String enabledPrefix(final @NotNull HighlightingType type) {
        return HIGHLIGHTING_PREFIX + type.name + ENABLED;
    }

    public @NotNull TokenStyle getTextSegmentHighlightingStyle() {
        return textSegmentHighlightingStyle;
    }

    public @Nullable TokenStyle getRegisterHighlightingStyle() {
        return registerHighlightingStyle;
    }

    public @Nullable TokenStyle getDataSegmentHighlightingStyle() {
        return dataSegmentHighlightingStyle;
    }

    public @NotNull TokenStyle getDelaySlotHighlightingStyle() {
        return delaySlotHighlightingStyle;
    }
    // endregion Preferences prefix methods

    public void saveSettings() {
        writeTokenStyleToPreferences(HighlightingType.TEXT_SEGMENT, this.textSegmentHighlightingStyle);
        writeTokenStyleToPreferences(HighlightingType.DELAY_SLOT, this.delaySlotHighlightingStyle);
        writeNullableTokenStyleToPreferences(HighlightingType.DATA_SEGMENT, this.dataSegmentHighlightingStyle);
        writeNullableTokenStyleToPreferences(HighlightingType.REGISTER, this.registerHighlightingStyle);
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
        submit(SettingsNotice.get());
    }

    // region Preference writing methods

    private void writeNullableTokenStyleToPreferences(final @NotNull HighlightingType type,
                                                      final @Nullable TokenStyle style) {
        if (style == null) {
            this.preferences.putBoolean(enabledPrefix(type), false);
        } else {
            this.preferences.putBoolean(enabledPrefix(type), true);
            writeTokenStyleToPreferences(type, style);
        }
    }

    private void writeTokenStyleToPreferences(final @NotNull HighlightingType type, final @NotNull TokenStyle style) {
        putNullableColor(foregroundPrefix(type), style.foreground());
        putNullableColor(backgroundPrefix(type), style.background());
        this.preferences.putBoolean(boldPrefix(type), style.isBold());
        this.preferences.putBoolean(italicPrefix(type), style.isItalic());
        this.preferences.putBoolean(underlinePrefix(type), style.isUnderline());
    }

    /**
     * If the value is null, save "null" to the preferences.
     * Otherwise, save the value to the preferences.
     */
    private void putNullableColor(final @NotNull String key, final @Nullable Color value) {
        final @NotNull String valueString;
        if (value == null) {
            valueString = "null";
        } else {
            valueString = getColorAsHexString(value);
        }
        this.preferences.put(key, valueString);
    }

    // endregion Preference writing methods

    // region Preference loading methods

    private void loadSettingsFromPreferences() {
        this.textSegmentHighlightingStyle = loadTokenStyleFromPreferences(HighlightingType.TEXT_SEGMENT);
        this.delaySlotHighlightingStyle = loadTokenStyleFromPreferences(HighlightingType.DELAY_SLOT);
        this.dataSegmentHighlightingStyle = loadNullableTokenStyleFromPreferences(HighlightingType.DATA_SEGMENT);
        this.registerHighlightingStyle = loadNullableTokenStyleFromPreferences(HighlightingType.REGISTER);
    }

    private @Nullable TokenStyle loadNullableTokenStyleFromPreferences(final @NotNull HighlightingType type) {
        final var enabled = this.preferences.getBoolean(enabledPrefix(type), false);
        if (!enabled) {
            return null;
        }
        return loadTokenStyleFromPreferences(type);
    }

    private @NotNull TokenStyle loadTokenStyleFromPreferences(final @NotNull HighlightingType type) {
        final var foreground = loadNullableColorFromPreferences(foregroundPrefix(type));
        final var background = loadNullableColorFromPreferences(backgroundPrefix(type));
        final var isBold = this.preferences.getBoolean(boldPrefix(type), false);
        final var isItalic = this.preferences.getBoolean(italicPrefix(type), false);
        final var isUnderline = this.preferences.getBoolean(underlinePrefix(type), false);
        return new TokenStyle(foreground, background, isBold, isItalic, isUnderline);
    }

    private @Nullable Color loadNullableColorFromPreferences(final @NotNull String key) {
        final var value = this.preferences.get(key, null);
        if (value == null || value.equals("null")) {
            return null;
        }
        try {
            return Color.decode(value);
        } catch (final NumberFormatException nfe) {
            LOGGER.error("Unable to decode color from preferences", nfe);
            return null;
        }
    }

    // endregion Preference loading methods

    public enum HighlightingType {
        TEXT_SEGMENT("Text"),
        DATA_SEGMENT("Data"),
        REGISTER("Register"),
        DELAY_SLOT("Delay");

        public final @NotNull String name;

        HighlightingType(final @NotNull String name) {
            this.name = name;
        }
    }
}
