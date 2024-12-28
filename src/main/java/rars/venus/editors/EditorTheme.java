package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class EditorTheme {
    public @NotNull HashMap<@NotNull RVTokenType, @NotNull TokenStyle> tokenStyles;
    public @NotNull Color backgroundColor;
    public @NotNull Color foregroundColor;
    public @NotNull Color lineHighlightColor;
    public @NotNull Color caretColor;
    public @NotNull Color selectionColor;

    public EditorTheme(
        final @NotNull Map<@NotNull RVTokenType, @NotNull TokenStyle> tokenStyles,
        final @NotNull Color backgroundColor,
        final @NotNull Color foregroundColor,
        final @NotNull Color lineHighlightColor,
        final @NotNull Color caretColor,
        final @NotNull Color selectionColor
    ) {
        this.tokenStyles = new HashMap<>(tokenStyles);
        Arrays.stream(RVTokenType.values())
            .forEach(tokenType ->
                this.tokenStyles.putIfAbsent(tokenType, TokenStyle.DEFAULT));
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.lineHighlightColor = lineHighlightColor;
        this.caretColor = caretColor;
        this.selectionColor = selectionColor;
    }
}
