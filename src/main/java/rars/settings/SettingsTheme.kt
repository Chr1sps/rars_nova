package rars.settings;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.venus.editors.EditorTheme;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static rars.settings.SettingsThemePresets.LIGHT_THEME;

public final class SettingsTheme implements Cloneable {
    public static @NotNull SettingsTheme DEFAULT_THEME = LIGHT_THEME;
    public @NotNull Color foregroundColor;
    public @NotNull Color backgroundColor;
    public @NotNull Color lineHighlightColor;
    public @NotNull Color selectionColor;
    public @NotNull Color caretColor;
    public @NotNull HashMap<@NotNull TokenSettingKey, @NotNull TokenStyle> tokenStyles;

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

    private static @NotNull HashMap<@NotNull TokenSettingKey, @NotNull TokenStyle> getFilledMap(final @NotNull Map<@NotNull TokenSettingKey, @NotNull TokenStyle> baseMap) {
        final var result = new HashMap<>(baseMap);
        Arrays.stream(TokenSettingKey.values()).forEach(key -> result.putIfAbsent(key, TokenStyle.DEFAULT));
        return result;
    }

    public @NotNull EditorTheme toEditorTheme() {
        return new EditorTheme(
            convertSettingsToThemeTokenStyles(this.tokenStyles),
            this.backgroundColor,
            this.foregroundColor,
            this.lineHighlightColor,
            this.caretColor,
            this.selectionColor
        );
    }

    @Override
    public @NotNull SettingsTheme clone() {
        final SettingsTheme clone;
        try {
            clone = (SettingsTheme) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.backgroundColor = new Color(this.backgroundColor.getRGB(), false);
        clone.foregroundColor = new Color(this.foregroundColor.getRGB(), false);
        clone.lineHighlightColor = new Color(this.lineHighlightColor.getRGB(), false);
        clone.caretColor = new Color(this.caretColor.getRGB(), false);
        clone.selectionColor = new Color(this.selectionColor.getRGB(), false);

        clone.tokenStyles = new HashMap<>(this.tokenStyles);

        return clone;
    }
}
