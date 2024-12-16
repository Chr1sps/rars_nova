package rars.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.riscv.lang.lexing.RVTokenType;
import rars.venus.editors.ColorScheme;
import rars.venus.editors.Theme;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static rars.util.Utils.getColorAsHexString;
import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public final class EditorThemeSettings {
    private static final Logger LOGGER = LogManager.getLogger();

    // region Preferences keys

    /**
     * Top level theme settings prefix.
     */
    private static final String THEME_PREFIX = "Theme";

    private static final String BACKGROUND = "Background";
    private static final String FOREGROUND = "Foreground";
    private static final String LINE_HIGHLIGHT = "LineHighlight";
    private static final String CARET = "Caret";
    private static final String SELECTION = "Selection";
    private static final String STYLES = "Styles";

    private static final String BOLD = "Bold";
    private static final String ITALIC = "Italic";
    private static final String UNDERLINE = "Underline";

    // endregion Preferences keys

    private final @NotNull Preferences preferences;
    private @NotNull Theme currentTheme;

    public EditorThemeSettings(final @NotNull Preferences preferences) {
        this.preferences = preferences;
        this.currentTheme = loadThemeFromPreferences();
    }

    // region Preferences prefix methods

    private static @NotNull String foregroundPrefix(final @NotNull RVTokenType tokenType) {
        return THEME_PREFIX + STYLES + tokenValue(tokenType) + FOREGROUND;
    }

    private static @NotNull String backgroundPrefix(final @NotNull RVTokenType tokenType) {
        return THEME_PREFIX + STYLES + tokenValue(tokenType) + BACKGROUND;
    }

    private static @NotNull String boldPrefix(final @NotNull RVTokenType tokenType) {
        return THEME_PREFIX + STYLES + tokenValue(tokenType) + BOLD;
    }

    private static @NotNull String italicPrefix(final @NotNull RVTokenType tokenType) {
        return THEME_PREFIX + STYLES + tokenValue(tokenType) + ITALIC;
    }

    private static @NotNull String underlinePrefix(final @NotNull RVTokenType tokenType) {
        return THEME_PREFIX + STYLES + tokenValue(tokenType) + UNDERLINE;
    }

    // endregion Preferences prefix methods

    /**
     * Returns the current theme in memory.
     *
     * @return the current theme
     */
    public @NotNull Theme getTheme() {
        return this.currentTheme;
    }

    /**
     * Sets the theme in memory to the given theme.
     * Does not save to preferences.
     *
     * @param newTheme the new theme to set
     */
    public void setTheme(final @NotNull Theme newTheme) {
        this.currentTheme = newTheme;
    }

    /**
     * Commits the theme currently in memory to the preferences.
     */
    public void saveThemeToPreferences() {
        this.preferences.put(THEME_PREFIX + BACKGROUND, getColorAsHexString(this.currentTheme.backgroundColor));
        this.preferences.put(THEME_PREFIX + FOREGROUND, getColorAsHexString(this.currentTheme.foregroundColor));
        this.preferences.put(THEME_PREFIX + LINE_HIGHLIGHT, getColorAsHexString(this.currentTheme.lineHighlightColor));
        this.preferences.put(THEME_PREFIX + CARET, getColorAsHexString(this.currentTheme.caretColor));
        this.preferences.put(THEME_PREFIX + SELECTION, getColorAsHexString(this.currentTheme.selectionColor));
        this.writeColorSchemeToPreferences(this.currentTheme.colorScheme);
        try {
            this.preferences.flush();
        } catch (final SecurityException se) {
            LOGGER.error("Unable to write to persistent storage for security reasons.");
        } catch (final BackingStoreException bse) {
            LOGGER.error("Unable to communicate with persistent storage.");
        }
    }

    // region Preference writing methods

    private void writeColorSchemeToPreferences(final @NotNull ColorScheme colorScheme) {
        for (final var entry : colorScheme.getEntries()) {
            final var type = entry.getKey();
            final var style = entry.getValue();
            this.writeTokenStyleToPreferences(type, style);
        }
    }

    private void writeTokenStyleToPreferences(final @NotNull RVTokenType type, final @NotNull TokenStyle style) {
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

    private @NotNull Theme loadThemeFromPreferences() {
        final var defaultTheme = Theme.getDefaultLightTheme();
        final var background = loadColorFromPreferences(THEME_PREFIX + BACKGROUND, defaultTheme.backgroundColor);
        final var foreground = loadColorFromPreferences(THEME_PREFIX + FOREGROUND, defaultTheme.foregroundColor);
        final var lineHighlight = loadColorFromPreferences(THEME_PREFIX + LINE_HIGHLIGHT,
                defaultTheme.lineHighlightColor);
        final var caret = loadColorFromPreferences(THEME_PREFIX + CARET, defaultTheme.caretColor);
        final var selection = loadColorFromPreferences(THEME_PREFIX + SELECTION, defaultTheme.selectionColor);
        final ColorScheme colorScheme = loadColorSchemeFromPreferences();
        return new Theme(colorScheme, background, foreground, lineHighlight, caret, selection);
    }

    private @NotNull ColorScheme loadColorSchemeFromPreferences() {
        final var styleMap = new HashMap<RVTokenType, TokenStyle>();
        for (final var type : RVTokenType.values()) {
            styleMap.put(type, loadTokenStyleFromPreferences(type));
        }
        return new ColorScheme(styleMap);
    }

    private @NotNull TokenStyle loadTokenStyleFromPreferences(final @NotNull RVTokenType type) {
        final var foreground = loadNullableColorFromPreferences(foregroundPrefix(type));
        final var background = loadNullableColorFromPreferences(backgroundPrefix(type));
        final var isBold = this.preferences.getBoolean(boldPrefix(type), false);
        final var isItalic = this.preferences.getBoolean(italicPrefix(type), false);
        final var isUnderline = this.preferences.getBoolean(underlinePrefix(type), false);
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
}
