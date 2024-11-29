package rars.venus.editors;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

@FunctionalInterface
public interface ColorSchemeConverter<Out> {
    @NotNull Out convert(final @NotNull ColorScheme colorScheme, final @NotNull Font baseFont);
}
