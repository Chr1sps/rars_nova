package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static rars.util.Utils.getColorAsHexString;

public final class EditorThemeSettings extends SettingsBase {
    private static final @NotNull Logger LOGGER = LogManager.getLogger();
    /**
     * Top level theme settings prefix.
     */
    private static final String THEME_PREFIX = "Theme";

    // region Preferences keys
    private static final String BACKGROUND = "Background";
    private static final String FOREGROUND = "Foreground";
    private static final String LINE_HIGHLIGHT = "LineHighlight";
    private static final String CARET = "Caret";
    private static final String SELECTION = "Selection";
    private static final String STYLES = "Styles";
    private static final String BOLD = "Bold";
    private static final String ITALIC = "Italic";
    private static final String UNDERLINE = "Underline";
    public static @NotNull EditorThemeSettings EDITOR_THEME_SETTINGS = new EditorThemeSettings(SETTINGS_PREFERENCES);

    // endregion Preferences keys
    private final @NotNull Preferences preferences;
    /**
     * The current theme in memory. You can make changes to this theme and then
     * call {@link #commitChanges()} to save the changes to the preferences.
     * You can also call {@link #discardChanges()} to revert the changes to the
     * state present in the preferences.
     */
    public @NotNull SettingsTheme currentTheme;
    private @NotNull SettingsTheme backupTheme;

    private EditorThemeSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        this.currentTheme = loadThemeFromPreferences();
        this.backupTheme = this.currentTheme.clone();
    }

    // region Preferences prefix methods

    private static @NotNull String foregroundPrefix(final @NotNull TokenSettingKey key) {
        return THEME_PREFIX + STYLES + key.ordinal() + FOREGROUND;
    }

    private static @NotNull String backgroundPrefix(final @NotNull TokenSettingKey key) {
        return THEME_PREFIX + STYLES + key.ordinal() + BACKGROUND;
    }

    private static @NotNull String boldPrefix(final @NotNull TokenSettingKey key) {
        return THEME_PREFIX + STYLES + key.ordinal() + BOLD;
    }

    private static @NotNull String italicPrefix(final @NotNull TokenSettingKey key) {
        return THEME_PREFIX + STYLES + key.ordinal() + ITALIC;
    }

    private static @NotNull String underlinePrefix(final @NotNull TokenSettingKey key) {
        return THEME_PREFIX + STYLES + key.ordinal() + UNDERLINE;
    }

    // endregion Preferences prefix methods

    /**
     * Commits the theme currently in memory to the preferences. If the commit
     * fails, the theme in memory will be reverted to the previous state.
     */
    public void commitChanges() {
        writeThemeToPreferences(this.currentTheme);
        try {
            this.preferences.flush();
            this.backupTheme = this.currentTheme;
            submit();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons. Reverting to previous settings.");
            // The reason why we need to write the backup theme to the preferences
            // is because the Preferences API implementations are free to flush
            // the changes to disk at any time.
            writeThemeToPreferences(this.backupTheme);
            this.currentTheme = this.backupTheme;
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage. Reverting to previous settings.");
            writeThemeToPreferences(this.backupTheme);
            this.currentTheme = this.backupTheme;
        }
    }

    /**
     * Restores the state of the theme settings in memory to the state in the
     * preferences.
     */
    public void discardChanges() {
        this.currentTheme = this.backupTheme;
    }

    // region Preference writing methods
    private void writeThemeToPreferences(final @NotNull SettingsTheme settingsTheme) {
        this.preferences.put(THEME_PREFIX + BACKGROUND, getColorAsHexString(settingsTheme.backgroundColor));
        this.preferences.put(THEME_PREFIX + FOREGROUND, getColorAsHexString(settingsTheme.foregroundColor));
        this.preferences.put(THEME_PREFIX + LINE_HIGHLIGHT, getColorAsHexString(settingsTheme.lineHighlightColor));
        this.preferences.put(THEME_PREFIX + CARET, getColorAsHexString(settingsTheme.caretColor));
        this.preferences.put(THEME_PREFIX + SELECTION, getColorAsHexString(settingsTheme.selectionColor));
        settingsTheme.tokenStyles.forEach(this::writeTokenStyleToPreferences);
    }

    private void writeTokenStyleToPreferences(final @NotNull TokenSettingKey key, final @NotNull TokenStyle style) {
        putNullableColor(foregroundPrefix(key), style.foreground());
        putNullableColor(backgroundPrefix(key), style.background());
        this.preferences.putBoolean(boldPrefix(key), style.isBold());
        this.preferences.putBoolean(italicPrefix(key), style.isItalic());
        this.preferences.putBoolean(underlinePrefix(key), style.isUnderline());
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

    private @NotNull SettingsTheme loadThemeFromPreferences() {
        final var defaultTheme = SettingsTheme.DEFAULT_THEME;
        final var background = loadColorFromPreferences(THEME_PREFIX + BACKGROUND, defaultTheme.backgroundColor);
        final var foreground = loadColorFromPreferences(THEME_PREFIX + FOREGROUND, defaultTheme.foregroundColor);
        final var lineHighlight = loadColorFromPreferences(THEME_PREFIX + LINE_HIGHLIGHT,
            defaultTheme.lineHighlightColor);
        final var caret = loadColorFromPreferences(THEME_PREFIX + CARET, defaultTheme.caretColor);
        final var selection = loadColorFromPreferences(THEME_PREFIX + SELECTION, defaultTheme.selectionColor);
        final var tokenStyles = loadTokenStylesFromPreferences(defaultTheme.tokenStyles);
        return new SettingsTheme(background, foreground, lineHighlight, caret, selection, tokenStyles);
    }

    private @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> loadTokenStylesFromPreferences(
        final @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> defaultColorScheme
    ) {
        final var styleMap = new HashMap<@NotNull TokenSettingKey, @NotNull TokenStyle>();
        for (final var type : TokenSettingKey.values()) {
            styleMap.put(type, loadTokenStyleFromPreferences(type, defaultColorScheme.get(type)));
        }
        return styleMap;
    }

    private @NotNull TokenStyle loadTokenStyleFromPreferences(final @NotNull TokenSettingKey key,
                                                              final @NotNull TokenStyle defaultStyle) {
        final var foreground = loadNullableColorFromPreferences(foregroundPrefix(key), defaultStyle.foreground());
        final var background = loadNullableColorFromPreferences(backgroundPrefix(key), defaultStyle.background());
        final var isBold = this.preferences.getBoolean(boldPrefix(key), defaultStyle.isBold());
        final var isItalic = this.preferences.getBoolean(italicPrefix(key), defaultStyle.isItalic());
        final var isUnderline = this.preferences.getBoolean(underlinePrefix(key), defaultStyle.isUnderline());
        return new TokenStyle(foreground, background, isBold, isItalic, isUnderline);
    }

    private @NotNull Color loadColorFromPreferences(final @NotNull String key, final @NotNull Color defaultValue) {
        final var value = this.preferences.get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Color.decode(value);
        } catch (final NumberFormatException nfe) {
            LOGGER.error("Unable to decode color from preferences", nfe);
            return defaultValue;
        }
    }

    private @Nullable Color loadNullableColorFromPreferences(final @NotNull String key,
                                                             final @Nullable Color defaultValue) {
        final var value = this.preferences.get(key, null);
        if (value == null) {
            return defaultValue;
        } else if (value.equals("null")) {
            return null;
        }
        try {
            return Color.decode(value);
        } catch (final NumberFormatException nfe) {
            LOGGER.error("Unable to decode color from preferences", nfe);
            return defaultValue;
        }
    }

    // endregion Preference loading methods
}
