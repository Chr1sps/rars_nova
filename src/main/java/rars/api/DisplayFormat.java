package rars.api;

import org.jetbrains.annotations.NotNull;

public enum DisplayFormat {
    DECIMAL,
    HEX,
    ASCII;

    @Override
    public @NotNull String toString() {
        return name().toLowerCase();
    }
}
