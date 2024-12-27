package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    private boolean isInitialized;
    private T value;
    private final @NotNull Supplier<T> supplier;
    public Lazy(final @NotNull Supplier<T> supplier) {
        this.isInitialized = false;
        this.value = null;
        this.supplier = supplier;
    }
    @Override
    public T get() {
        if (!isInitialized) {
            value = supplier.get();
            isInitialized = true;
        }
        return value;
    }
    public static <T> @NotNull Lazy<T> of(final @NotNull Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }
}
