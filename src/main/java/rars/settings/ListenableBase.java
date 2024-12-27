package rars.settings;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * A base class for objects that can have listeners attached to them.
 *
 * @param <T> The type of this object. It is *required* that this type parameter
 *            be the same as the class that extends this class.
 */
public abstract class ListenableBase<T extends ListenableBase<T>> {
    private final @NotNull HashSet<Consumer<T>> listeners;

    protected ListenableBase() {
        listeners = new HashSet<>();
    }

    public void addChangeListener(final @NotNull Consumer<T> listener) {
        this.listeners.add(listener);
    }

    public void addChangeListener(final @NotNull Consumer<T> listener, final boolean runImmediately) {
        if (runImmediately) {
            // noinspection unchecked
            listener.accept((T) this);
        }
        this.listeners.add(listener);
    }

    public void removeChangeListener(final @NotNull Consumer<T> listener) {
        this.listeners.remove(listener);
    }

    protected void submit() {
        for (final var listener : listeners) {
            // noinspection unchecked
            listener.accept((T) this);
        }
    }
}
