package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import rars.util.Lazy;

import java.awt.*;

public final class Theme {
    private static final @NotNull Lazy<Theme> defaultDarkTheme = Lazy.of(() -> {
        final var defaultScheme = ColorScheme.getDefaultDarkScheme();
        final var backgroundColor = new Color(47, 47, 47);
        final var foregroundColor = Color.WHITE;
        final var lineHighlightColor = new Color(132, 176, 250, 48);
        final var caretColor = Color.LIGHT_GRAY;
        final var selectionColor = new Color(83, 146, 229, 48);
        return new Theme(
                defaultScheme,
                backgroundColor,
                foregroundColor,
                lineHighlightColor,
                caretColor,
                selectionColor
        );

    });
    private static final @NotNull Lazy<Theme> defaultLightTheme = Lazy.of(() -> {
        final var defaultScheme = ColorScheme.getDefaultLightScheme();
        final var backgroundColor = Color.WHITE;
        final var foregroundColor = Color.BLACK;
        final var lineHighlightColor = new Color(132, 176, 250, 128);
        final var caretColor = Color.DARK_GRAY;
        final var selectionColor = new Color(83, 146, 229, 128);
        return new Theme(
                defaultScheme,
                backgroundColor,
                foregroundColor,
                lineHighlightColor,
                caretColor,
                selectionColor
        );
    });

    public @NotNull ColorScheme colorScheme;
    public @NotNull Color backgroundColor;
    public @NotNull Color foregroundColor;
    public @NotNull Color lineHighlightColor;
    public @NotNull Color caretColor;
    public @NotNull Color selectionColor;

    public Theme(
            final @NotNull ColorScheme colorScheme,
            final @NotNull Color backgroundColor,
            final @NotNull Color foregroundColor,
            final @NotNull Color lineHighlightColor,
            final @NotNull Color caretColor,
            final @NotNull Color selectionColor
    ) {
        this.colorScheme = colorScheme;
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.lineHighlightColor = lineHighlightColor;
        this.caretColor = caretColor;
        this.selectionColor = selectionColor;
    }

    public static @NotNull Theme getDefaultLightTheme() {
        return defaultLightTheme.get();
    }

    public static @NotNull Theme getDefaultDarkTheme() {
        return defaultDarkTheme.get();
    }
}
