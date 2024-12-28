package rars.settings;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * A base class for objects that can have listeners attached to them.
 */
public abstract class ListenableBase {
    private final @NotNull HashSet<Runnable> listeners;

    protected ListenableBase() {
        listeners = new HashSet<>();
    }

    public void addChangeListener(final @NotNull Runnable listener) {
        this.listeners.add(listener);
    }

    public void addChangeListener(final @NotNull Runnable listener, final boolean runImmediately) {
        if (runImmediately) {
            // noinspection unchecked
            listener.run();
        }
        this.listeners.add(listener);
    }

    public void removeChangeListener(final @NotNull Runnable listener) {
        this.listeners.remove(listener);
    }

    protected void submit() {
        for (final var listener : listeners) {
            // noinspection unchecked
            listener.run();
        }
    }
}
