package rars.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record Pair<K, V>(K first, V second) implements Map.Entry<K, V> {
    @Contract("_, _ -> new")
    public static <K, V> @NotNull Pair<K, V> of(final K first, final V second) {
        return new Pair<>(first, second);
    }

    @Override
    public K getKey() {
        return first;
    }

    @Override
    public V getValue() {
        return second;
    }

    @Override
    public V setValue(final V value) {
        throw new UnsupportedOperationException();
    }
}
