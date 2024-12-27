package rars.venus.editors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public record TokenStyle(
        @Nullable Color foreground,
        @Nullable Color background,
        boolean isBold,
        boolean isItalic,
        boolean isUnderline
) {
    public static final Color DEFAULT_FOREGROUND = null;
    public static final Color DEFAULT_BACKGROUND = null;

    public static final TokenStyle DEFAULT = new TokenStyle(DEFAULT_FOREGROUND, DEFAULT_BACKGROUND, false, false,
            false);

    public static @NotNull TokenStyle plain(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, false, false, false);
    }

    public static @NotNull TokenStyle bold(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, true, false, false);
    }

    public static @NotNull TokenStyle italic(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, false, true, false);
    }
    public static @NotNull TokenStyle underline(final @NotNull Color foreground) {
        return new TokenStyle(foreground, DEFAULT_BACKGROUND, false, false, true);
    }
}
