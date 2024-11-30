package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.util.Lazy;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class ColorScheme {
    private static final @NotNull Lazy<ColorScheme> defaultDarkScheme = Lazy.of(() -> {
        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 159, 0);
        final var directiveColor = new Color(0, 183, 159);
        final var operatorColor = new Color(128, 64, 64);
        final var registerColor = new Color(222, 134, 0);
        final var numberColor = new Color(159, 57, 255);
        final var stringColor = new Color(255, 0, 176);
        final var labelColor = new Color(161, 161, 161);
        final var instructionColor = new Color(80, 80, 253);
        final var macroParameterColor = new Color(255, 70, 38);

        final var errorStyle = TokenStyle.underline(errorColor);

        final var styleMap = ofEntries(
                entry(RVTokenType.ERROR, errorStyle),

                entry(RVTokenType.COMMENT, TokenStyle.italic(commentColor)),
                entry(RVTokenType.DIRECTIVE, TokenStyle.plain(directiveColor)),
                entry(RVTokenType.OPERATOR, TokenStyle.plain(operatorColor)),

                entry(RVTokenType.REGISTER_NAME, TokenStyle.plain(registerColor)),

                entry(RVTokenType.IDENTIFIER, TokenStyle.DEFAULT),

                entry(RVTokenType.INTEGER, TokenStyle.plain(numberColor)),
                entry(RVTokenType.FLOATING, TokenStyle.plain(numberColor)),
                entry(RVTokenType.STRING, TokenStyle.plain(stringColor)),
                entry(RVTokenType.CHAR, TokenStyle.plain(stringColor)),
                entry(RVTokenType.UNFINISHED_STRING, errorStyle),
                entry(RVTokenType.UNFINISHED_CHAR, errorStyle),

                entry(RVTokenType.LABEL, TokenStyle.plain(labelColor)),
                entry(RVTokenType.INSTRUCTION, TokenStyle.bold(instructionColor)),

                entry(RVTokenType.MACRO_PARAMETER, TokenStyle.plain(macroParameterColor))
        );
        return new ColorScheme(styleMap);
    });
    private static final @NotNull Lazy<ColorScheme> defaultLightScheme = Lazy.of(() -> {
        final var errorColor = new Color(255, 0, 0);
        final var commentColor = new Color(0, 128, 0);
        final var directiveColor = new Color(0, 134, 116);
        final var operatorColor = new Color(128, 64, 64);
        final var registerColor = new Color(169, 103, 0);
        final var numberColor = new Color(100, 0, 200);
        final var stringColor = new Color(175, 1, 121);
        final var labelColor = new Color(128, 128, 128);
        final var instructionColor = new Color(33, 33, 161);
        final var macroParameterColor = new Color(178, 49, 27);

        final var errorStyle = TokenStyle.underline(errorColor);

        final var styleMap = ofEntries(
                entry(RVTokenType.ERROR, errorStyle),

                entry(RVTokenType.COMMENT, TokenStyle.italic(commentColor)),
                entry(RVTokenType.DIRECTIVE, TokenStyle.plain(directiveColor)),
                entry(RVTokenType.OPERATOR, TokenStyle.plain(operatorColor)),

                entry(RVTokenType.REGISTER_NAME, TokenStyle.plain(registerColor)),

                entry(RVTokenType.IDENTIFIER, TokenStyle.DEFAULT),

                entry(RVTokenType.INTEGER, TokenStyle.plain(numberColor)),
                entry(RVTokenType.FLOATING, TokenStyle.plain(numberColor)),
                entry(RVTokenType.STRING, TokenStyle.plain(stringColor)),
                entry(RVTokenType.CHAR, TokenStyle.plain(stringColor)),
                entry(RVTokenType.UNFINISHED_STRING, errorStyle),
                entry(RVTokenType.UNFINISHED_CHAR, errorStyle),

                entry(RVTokenType.LABEL, TokenStyle.plain(labelColor)),
                entry(RVTokenType.INSTRUCTION, TokenStyle.bold(instructionColor)),

                entry(RVTokenType.MACRO_PARAMETER, TokenStyle.plain(macroParameterColor))
        );
        return new ColorScheme(styleMap);
    });

    private final HashMap<RVTokenType, TokenStyle> styles;

    public ColorScheme(final @NotNull Map<RVTokenType, TokenStyle> map) {
        this.styles = new HashMap<>(map);
        // fill in the missing token types with the default style
        Arrays.stream(RVTokenType.values())
                .forEach(tokenType ->
                        styles.putIfAbsent(tokenType, TokenStyle.DEFAULT));
    }

    public static @NotNull ColorScheme getDefaultLightScheme() {
        return defaultLightScheme.get();
    }

    public static @NotNull ColorScheme getDefaultDarkScheme() {
        return defaultDarkScheme.get();
    }

    public @NotNull TokenStyle getStyle(final @NotNull RVTokenType tokenType) {
        return styles.get(tokenType);
    }

    public @NotNull Set<Map.Entry<RVTokenType, TokenStyle>> getEntries() {
        return styles.entrySet();
    }

    public void setStyle(final @NotNull RVTokenType tokenType, final @NotNull TokenStyle tokenStyle) {
        styles.put(tokenType, tokenStyle);
    }
}
