package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class Lazy<T> implements Supplier<T> {
    private final @NotNull Supplier<T> supplier;
    private boolean isInitialized;
    private T value;

    public Lazy(final @NotNull Supplier<T> supplier) {
        this.isInitialized = false;
        this.value = null;
        this.supplier = supplier;
    }

    public static <T> @NotNull Lazy<T> of(final @NotNull Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public synchronized T get() {
        if (!isInitialized) {
            value = supplier.get();
            isInitialized = true;
        }
        return value;
    }
}
