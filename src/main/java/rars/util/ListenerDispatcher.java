package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * A simple class modeling a type of reactive programming. The dispatcher
 * is used to send data (using the {@link #dispatch} method) to the
 * listening consumers.
 *
 * @param <Data>
 *     type of objects being sent via the dispatcher.
 */
public final class ListenerDispatcher<Data> {

    private final @NotNull HashSet<@NotNull Handle> handles;
    private final @NotNull Lock listenersLock;
    private final @NotNull Hook hook;

    /**
     * Creates a new dispatcher.
     */
    public ListenerDispatcher() {
        this.handles = new HashSet<>();
        this.listenersLock = new ReentrantReadWriteLock().writeLock();
        this.hook = new Hook();
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
            for (final var handle : this.handles) {
                if (!handle.isCancelled) {
                    handle.innerListener.accept(data);
                }
            }
            this.handles.removeIf(handle -> handle.isCancelled);
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
        return hook;
    }

    /**
     * Each time one subscribes to the given {@link Hook} object (via the
     * {@link Hook#subscribe} method, one receives a unique handle that
     * serves as a token referencing the subscription of the listener
     * passed to the method. This handle can then be used to cancel the
     * existing subscription and to stop listening to a given data
     * emitter (via a hook's {@link Hook#unsubscribe(Handle)} method).
     */
    public final class Handle {
        private final @NotNull Consumer<? super Data> innerListener;
        private boolean isCancelled;

        private Handle(final @NotNull Consumer<? super Data> innerListener) {
            this.innerListener = innerListener;
            this.isCancelled = false;
        }
    }

    /**
     * Inner class that exposes an entry point for listeners to subscribe to.
     * This is done to separate the publisher API from the listeners.
     */
    public final class Hook {
        private Hook() {

        }

        /**
         * Adds a listener that will be run when the hook is notified.
         *
         * @param listener
         *     The listener to add.
         */
        public @NotNull Handle subscribe(final @NotNull Consumer<? super Data> listener) {
            final var handle = new Handle(listener);
            ListenerDispatcher.this.listenersLock.lock();
            try {
                ListenerDispatcher.this.handles.add(handle);
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
            return handle;
        }

        public void unsubscribe(final @NotNull Handle handle) {
            ListenerDispatcher.this.listenersLock.lock();
            try {
                for (final var myHandle : ListenerDispatcher.this.handles) {
                    if (myHandle.equals(handle)) {
                        myHandle.isCancelled = true;
                        return;
                    }
                }
                throw new IllegalArgumentException("Handle not found in this hook.");
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
        }
    }
}
