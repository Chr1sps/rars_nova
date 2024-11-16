package rars.venus.editors;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ColorSchemeConverter<Out> {
    @NotNull Out convert(final @NotNull ColorScheme colorScheme);
}
