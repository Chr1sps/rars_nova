package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.venus.editors.Theme;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

public final class SettingsTheme {
    public static @NotNull SettingsTheme DEFAULT_LIGHT_THEME = new SettingsTheme(
        Color.WHITE,
        Color.BLACK,
        new Color(204, 204, 204),
        Color.DARK_GRAY,
        new Color(169, 191, 255),
        getDefaultLightTokenStyles()
    );
    public static @NotNull SettingsTheme DEFAULT_DARK_THEME = new SettingsTheme(
        new Color(31, 31, 31),
        Color.WHITE,
        new Color(47, 47, 47),
        Color.LIGHT_GRAY,
        new Color(61, 61, 61),
        getDefaultDarkTokenStyles()
    );
    public static @NotNull SettingsTheme DEFAULT_THEME = DEFAULT_DARK_THEME;
    public @NotNull Color foregroundColor;
    public @NotNull Color backgroundColor;
    public @NotNull Color lineHighlightColor;
    public @NotNull Color selectionColor;
    public @NotNull Color caretColor;
    public @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> tokenStyles;

    public SettingsTheme(
        final @NotNull Color backgroundColor,
        final @NotNull Color foregroundColor,
        final @NotNull Color lineHighlightColor,
        final @NotNull Color caretColor,
        final @NotNull Color selectionColor,
        final @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> tokenStyles
    ) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.lineHighlightColor = lineHighlightColor;
        this.caretColor = caretColor;
        this.selectionColor = selectionColor;
        this.tokenStyles = getFilledMap(tokenStyles);
    }

    public static @NotNull SettingsTheme getDefaultTheme(final boolean isDarkMode) {
        return isDarkMode ? DEFAULT_DARK_THEME : DEFAULT_LIGHT_THEME;
    }

    private static @NotNull Map<@NotNull RVTokenType, @NotNull TokenStyle> convertSettingsToThemeTokenStyles(
        final @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> tokenStyles
    ) {
        final var result = new HashMap<@NotNull RVTokenType, @NotNull TokenStyle>();
        tokenStyles.forEach((key, style) ->
            TokenSettingKey.getTokenTypesForSetting(key)
                .forEach(tokenType -> result.put(tokenType, style)));
        return result;
    }

    private static @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> getFilledMap(final @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> baseMap) {
        final var result = new HashMap<>(baseMap);
        Arrays.stream(TokenSettingKey.values()).forEach(key -> result.putIfAbsent(key, TokenStyle.DEFAULT));
        return result;
    }

    private static @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> getTokenSettingKeyTokenStyleMap(
        final Color errorColor,
        final Color commentColor,
        final Color directiveColor,
        final Color registerColor,
        final Color numberColor,
        final Color stringColor,
        final Color labelColor,
        final Color instructionColor,
        final Color macroParameterColor) {
        final var result = Map.ofEntries(
            entry(TokenSettingKey.ERROR, TokenStyle.underline(errorColor)),
            entry(TokenSettingKey.COMMENT, TokenStyle.italic(commentColor)),
            entry(TokenSettingKey.DIRECTIVE, TokenStyle.bold(directiveColor)),

            entry(TokenSettingKey.REGISTER_NAME, TokenStyle.bold(registerColor)),

            entry(TokenSettingKey.NUMBER, TokenStyle.bold(numberColor)),
            entry(TokenSettingKey.STRING, TokenStyle.bold(stringColor)),

            entry(TokenSettingKey.LABEL, TokenStyle.bold(labelColor)),
            entry(TokenSettingKey.INSTRUCTION, TokenStyle.bold(instructionColor)),

            entry(TokenSettingKey.MACRO_PARAMETER, TokenStyle.bold(macroParameterColor))
        );
        return getFilledMap(result);
    }

    private static @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> getDefaultLightTokenStyles() {
        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 128, 0);
        final var directiveColor = new Color(0, 134, 116);
        final var registerColor = new Color(169, 103, 0);
        final var numberColor = new Color(100, 0, 200);
        final var stringColor = new Color(175, 1, 121);
        final var labelColor = new Color(128, 128, 128);
        final var instructionColor = new Color(33, 33, 161);
        final var macroParameterColor = new Color(178, 49, 27);

        return getTokenSettingKeyTokenStyleMap(
            errorColor,
            commentColor,
            directiveColor,
            registerColor,
            numberColor,
            stringColor,
            labelColor,
            instructionColor,
            macroParameterColor
        );
    }

    private static @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> getDefaultDarkTokenStyles() {
        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 159, 0);
        final var directiveColor = new Color(0, 183, 159);
        final var registerColor = new Color(222, 134, 0);
        final var numberColor = new Color(159, 57, 255);
        final var stringColor = new Color(255, 0, 176);
        final var labelColor = new Color(161, 161, 161);
        final var instructionColor = new Color(80, 80, 253);
        final var macroParameterColor = new Color(255, 70, 38);

        return getTokenSettingKeyTokenStyleMap(
            errorColor,
            commentColor,
            directiveColor,
            registerColor,
            numberColor,
            stringColor,
            labelColor,
            instructionColor,
            macroParameterColor
        );
    }

    public @NotNull Theme toTheme() {
        return new Theme(
            convertSettingsToThemeTokenStyles(this.tokenStyles),
            this.backgroundColor,
            this.foregroundColor,
            this.lineHighlightColor,
            this.caretColor,
            this.selectionColor
        );
    }
}
