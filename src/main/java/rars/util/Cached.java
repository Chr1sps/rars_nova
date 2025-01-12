package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class Cached<T> implements Supplier<T> {
    private final @NotNull Supplier<T> supplier;
    private boolean isInitialized;
    private T value;

    public Cached(final @NotNull Supplier<T> supplier) {
        this.isInitialized = false;
        this.value = null;
        this.supplier = supplier;
    }

    public static <T> @NotNull Cached<T> of(final @NotNull Supplier<T> supplier) {
        return new Cached<>(supplier);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public synchronized T get() {
        if (!isInitialized) {
            value = supplier.get();
            isInitialized = true;
        }
        return value;
    }

    public synchronized void invalidate() {
        isInitialized = false;
    }
}
