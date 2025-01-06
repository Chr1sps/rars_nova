package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class ListenerDispatcher<Data> {

    private final @NotNull HashSet<@NotNull Consumer<? super Data>> listeners;
    private final @NotNull Lock listenersLock;

    /**
     * Creates a new dispatcher.
     */
    public ListenerDispatcher() {
        this.listeners = new HashSet<>();
        this.listenersLock = new ReentrantReadWriteLock().writeLock();
    }

    /**
     * Notifies all listeners with the given data.
     *
     * @param data
     *     The data to pass to the listeners.
     */
    public void dispatch(final Data data) {
        this.listenersLock.lock();
        try {
            for (final var listener : this.listeners) {
                listener.accept(data);
            }
        } finally {
            this.listenersLock.unlock();
        }
    }

    /**
     * Returns a hook that listeners can subscribe to.
     *
     * @return A hook that listeners can subscribe to.
     */
    public @NotNull Hook getHook() {
        return new Hook();
    }

    /**
     * Inner class that exposes an entry point for listeners to subscribe to.
     * This is done to separate the publisher API from the listeners.
     */
    public class Hook {
        private Hook() {

        }

        /**
         * Adds a listener that will be run when the hook is notified.
         *
         * @param listener
         *     The listener to add.
         */
        public void subscribe(final @NotNull Consumer<? super Data> listener) {
            ListenerDispatcher.this.listenersLock.lock();
            try {
                ListenerDispatcher.this.listeners.add(listener);
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
        }

        public void unsubscribe(final @NotNull Consumer<? super Data> listener) {
            ListenerDispatcher.this.listenersLock.lock();
            try {
                ListenerDispatcher.this.listeners.remove(listener);
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
        }
    }
}
