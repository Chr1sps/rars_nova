package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public record TokenStyle(
        @NotNull Color foreground,
        @Nullable Color background,
        boolean isBold,
        boolean isItalic,
        boolean isUnderline
) {
    public static final Color DEFAULT_FOREGROUND = Color.BLACK;
    public static final Color DEFAULT_BACKGROUND = null;

    public TokenStyle() {
        this(DEFAULT_FOREGROUND, DEFAULT_BACKGROUND, false, false, false);
    }

    public static @NotNull TokenStyle plain(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, false, false, false);
    }

    public static @NotNull TokenStyle bold(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, true, false, false);
    }

    public static @NotNull TokenStyle italic(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, false, true, false);
    }
}
