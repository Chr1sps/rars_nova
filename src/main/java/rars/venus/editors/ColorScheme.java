package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class ColorScheme {
    private static final Map<RVTokenType, TokenStyle> defaultStyles = ofEntries(
            entry(RVTokenType.COMMENT, new TokenStyle(Color.GREEN))
    );
    private static final ColorScheme defaultScheme = new ColorScheme(defaultStyles);
    private static final TokenStyle defaultStyle = new TokenStyle();
    private final HashMap<RVTokenType, TokenStyle> styles;

    public ColorScheme(final @NotNull Map<RVTokenType, TokenStyle> map) {
        this.styles = new HashMap<>(map);
    }

    public static @NotNull ColorScheme getDefaultScheme() {
        return defaultScheme;
    }

    public @NotNull TokenStyle getStyle(final @NotNull RVTokenType tokenType) {
        if (styles.containsKey(tokenType)) {
            return styles.get(tokenType);
        }
        return defaultStyle;
    }

    public @NotNull Map<RVTokenType, TokenStyle> getStyleMap() {
        return styles;
    }

    public void setStyle(final @NotNull RVTokenType tokenType, final @NotNull TokenStyle tokenStyle) {
        styles.put(tokenType, tokenStyle);
    }
}
