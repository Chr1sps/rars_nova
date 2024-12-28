package rars.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static java.util.Map.entry;

public final class SettingsThemePresets {
    public static final @NotNull SettingsTheme LIGHT_THEME = buildTheme(theme -> {
        theme.foregroundColor = Color.BLACK;
        theme.backgroundColor = Color.WHITE;
        theme.lineHighlightColor = new Color(242, 242, 242);
        theme.selectionColor = new Color(229, 235, 235);
        theme.caretColor = Color.DARK_GRAY;

        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 128, 0);
        final var directiveColor = new Color(0, 134, 116);
        final var registerColor = new Color(169, 103, 0);
        final var numberColor = new Color(100, 0, 200);
        final var stringColor = new Color(175, 1, 121);
        final var labelColor = new Color(128, 128, 128);
        final var instructionColor = new Color(33, 33, 161);
        final var macroParameterColor = new Color(178, 49, 27);

        theme.tokenStyles.putAll(Map.ofEntries(
            entry(TokenSettingKey.ERROR, TokenStyle.underline(errorColor)),
            entry(TokenSettingKey.COMMENT, TokenStyle.italic(commentColor)),
            entry(TokenSettingKey.DIRECTIVE, TokenStyle.bold(directiveColor)),

            entry(TokenSettingKey.REGISTER_NAME, TokenStyle.bold(registerColor)),

            entry(TokenSettingKey.NUMBER, TokenStyle.bold(numberColor)),
            entry(TokenSettingKey.STRING, TokenStyle.bold(stringColor)),

            entry(TokenSettingKey.LABEL, TokenStyle.bold(labelColor)),
            entry(TokenSettingKey.INSTRUCTION, TokenStyle.bold(instructionColor)),

            entry(TokenSettingKey.MACRO_PARAMETER, TokenStyle.bold(macroParameterColor))
        ));
        return theme;
    });
    public static final @NotNull SettingsTheme DARK_THEME = buildTheme(theme -> {
        theme.foregroundColor = Color.WHITE;
        theme.backgroundColor = new Color(31, 31, 31);
        theme.lineHighlightColor = new Color(47, 47, 47);
        theme.selectionColor = new Color(61, 61, 61);
        theme.caretColor = Color.LIGHT_GRAY;

        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 159, 0);
        final var directiveColor = new Color(0, 183, 159);
        final var registerColor = new Color(222, 134, 0);
        final var numberColor = new Color(159, 57, 255);
        final var stringColor = new Color(255, 0, 176);
        final var labelColor = new Color(161, 161, 161);
        final var instructionColor = new Color(80, 80, 253);
        final var macroParameterColor = new Color(255, 70, 38);

        theme.tokenStyles.putAll(Map.ofEntries(
            entry(TokenSettingKey.ERROR, TokenStyle.underline(errorColor)),
            entry(TokenSettingKey.COMMENT, TokenStyle.italic(commentColor)),
            entry(TokenSettingKey.DIRECTIVE, TokenStyle.bold(directiveColor)),

            entry(TokenSettingKey.REGISTER_NAME, TokenStyle.bold(registerColor)),

            entry(TokenSettingKey.NUMBER, TokenStyle.bold(numberColor)),
            entry(TokenSettingKey.STRING, TokenStyle.bold(stringColor)),

            entry(TokenSettingKey.LABEL, TokenStyle.bold(labelColor)),
            entry(TokenSettingKey.INSTRUCTION, TokenStyle.bold(instructionColor)),

            entry(TokenSettingKey.MACRO_PARAMETER, TokenStyle.bold(macroParameterColor))
        ));
        return theme;
    });
    public static final @NotNull
    @Unmodifiable List<@NotNull ThemeEntry> THEMES = List.of(
        new ThemeEntry("Default light", LIGHT_THEME),
        new ThemeEntry("Default dark", DARK_THEME)
    );

    private static @NotNull SettingsTheme buildTheme(final @NotNull UnaryOperator<SettingsTheme> builderFunction) {
        return builderFunction.apply(new SettingsTheme(
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Map.of()
        ));
    }

    public record ThemeEntry(@NotNull String name, @NotNull SettingsTheme theme) {
    }
}
