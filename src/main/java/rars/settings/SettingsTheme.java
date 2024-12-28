package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.venus.editors.Theme;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static rars.settings.SettingsThemePresets.LIGHT_THEME;

public final class SettingsTheme {
    public static @NotNull SettingsTheme DEFAULT_THEME = LIGHT_THEME;
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
