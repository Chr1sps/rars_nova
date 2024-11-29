package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.riscv.lang.lexing.RVTokenType;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class ColorScheme {
    private static final TokenStyle defaultStyle = new TokenStyle();
    private static final ColorScheme defaultScheme = new ColorScheme(getDefaultStylesMap());
    private final HashMap<RVTokenType, TokenStyle> styles;

    public ColorScheme(final @NotNull Map<RVTokenType, TokenStyle> map) {
        this.styles = new HashMap<>(map);
        // fill in the missing token types with the default style
        Arrays.stream(RVTokenType.values())
                .forEach(tokenType ->
                        styles.putIfAbsent(tokenType, defaultStyle));
    }

    private static @NotNull @Unmodifiable Map<RVTokenType, TokenStyle> getDefaultStylesMap() {
        final var errorColor = new Color(160, 0, 0);
        final var commentColor = new Color(0, 128, 0);
        final var directiveColor = new Color(0, 191, 165);
        final var operatorColor = new Color(128, 64, 64);
        final var registerColor = new Color(255, 153, 0);
        final var identifierColor = Color.BLACK;
        final var numberColor = new Color(100, 0, 200);
        final var stringColor = new Color(220, 0, 156);
        final var labelColor = new Color(128, 128, 128);
        final var instructionColor = Color.BLUE;
        final var macroParameterColor = new Color(255, 114, 38);

        return ofEntries(
                entry(RVTokenType.ERROR, TokenStyle.plain(errorColor)),

                entry(RVTokenType.COMMENT, TokenStyle.italic(commentColor)),
                entry(RVTokenType.DIRECTIVE, TokenStyle.plain(directiveColor)),
                entry(RVTokenType.OPERATOR, TokenStyle.plain(operatorColor)),

                entry(RVTokenType.REGISTER_NAME, TokenStyle.plain(registerColor)),

                entry(RVTokenType.IDENTIFIER, TokenStyle.plain(identifierColor)),

                entry(RVTokenType.INTEGER, TokenStyle.plain(numberColor)),
                entry(RVTokenType.FLOATING, TokenStyle.plain(numberColor)),
                entry(RVTokenType.STRING, TokenStyle.plain(stringColor)),
                entry(RVTokenType.CHAR, TokenStyle.plain(stringColor)),
                entry(RVTokenType.UNFINISHED_STRING, TokenStyle.plain(errorColor)),
                entry(RVTokenType.UNFINISHED_CHAR, TokenStyle.plain(errorColor)),

                entry(RVTokenType.LABEL, TokenStyle.plain(labelColor)),
                entry(RVTokenType.INSTRUCTION, TokenStyle.bold(instructionColor)),

                entry(RVTokenType.MACRO_PARAMETER, TokenStyle.plain(macroParameterColor))
        );
    }

    public static @NotNull ColorScheme getDefaultScheme() {
        return defaultScheme;
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
