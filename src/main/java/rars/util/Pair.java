package rars.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Pair<K, V>(K first, V second) {
    @Contract("_, _ -> new")
    public static <K, V> @NotNull Pair<K, V> of(final K first, final V second) {
        return new Pair<>(first, second);
    }
}
