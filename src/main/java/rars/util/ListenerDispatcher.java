package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private final @NotNull HashSet<@NotNull Handle<? super Data>> handles;
    private final @NotNull Lock listenersLock;

    /**
     * Creates a new dispatcher.
     */
    public ListenerDispatcher() {
        this.handles = new HashSet<>();
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
            for (final var handle : this.handles) {
                if (!Handle.isCancelled(handle)) {
                    // handle.innerListener.accept(data);
                    Handle.accept(handle, data);
                }
            }
            this.handles.removeIf(Handle::isCancelled);
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
     * Each time one subscribes to the given {@link Hook} object (via the
     * {@link Hook#subscribe} method, one receives a unique handle that
     * serves as a token referencing the subscription of the listener
     * passed to the method. This handle can then be used to cancel the
     * existing subscription and to stop listening to a given data
     * emitter (via a hook's {@link Hook#unsubscribe(Handle)} method).
     * @param <Data>
     */
    @SuppressWarnings("unused")
    public sealed interface Handle<Data> {

        /**
         * Creates a handle that combines other handles. The handles
         * passed into the function are consumed - their use after the call
         * is invalid.
         */
        static <T> @NotNull Handle<T> compound(final @NotNull List<@NotNull Handle<T>> handles) {
            return switch (handles.size()) {
                case 0 -> throw new IllegalArgumentException("Must have at least one listener");
                case 1 -> handles.getFirst();
                default -> {
                    final var handleSet = new HashSet<@NotNull Consumer<? super T>>();
                    for (final var handle : handles) {
                        switch (handle) {
                            case ListenerDispatcher.CompoundHandle<? super T> v -> handleSet.addAll(v.callbacks);
                            case ListenerDispatcher.HandleImpl<? super T> v -> handleSet.add(v.innerListener);
                        }
                    }

                    yield new CompoundHandle<>(handleSet);
                }
            };
        }

        /**
         * Creates a simple handle that uses the passed callback on each data reception.
         */
        static <T> @NotNull Handle<T> create(final @NotNull Consumer<? super T> listener) {
            return new HandleImpl<>(listener);
        }

        private static <T> void accept(final @NotNull Handle<T> handle, final @NotNull T data) {
            switch (handle) {
                case ListenerDispatcher.CompoundHandle<? super T> compound -> {
                    for (final var callback : compound.callbacks) {
                        callback.accept(data);
                    }
                }
                case ListenerDispatcher.HandleImpl<? super T> simple -> {
                    if (!Handle.isCancelled(simple)) {
                        simple.innerListener.accept(data);
                    }
                }
            }
        }

        private static boolean isCancelled(final @NotNull Handle<?> handle) {
            return switch (handle) {
                case ListenerDispatcher.CompoundHandle<?> v -> v.isCancelled;
                case ListenerDispatcher.HandleImpl<?> v -> v.isCancelled;
            };
        }

        private static <T> void cancel(final @NotNull Handle<T> handle) {
            switch (handle) {
                case ListenerDispatcher.CompoundHandle<? super T> compound -> compound.isCancelled = true;
                case ListenerDispatcher.HandleImpl<? super T> simple -> simple.isCancelled = true;
            }
        }
    }

    private static final class HandleImpl<T> implements Handle<T> {
        private final @NotNull Consumer<? super T> innerListener;
        private boolean isCancelled;

        private HandleImpl(final @NotNull Consumer<? super T> innerListener) {
            this.innerListener = innerListener;
            this.isCancelled = false;
        }
    }

    private static final class CompoundHandle<T> implements Handle<T> {
        final @NotNull Set<@NotNull Consumer<? super T>> callbacks;
        private boolean isCancelled;

        CompoundHandle(final @NotNull Set<@NotNull Consumer<? super T>> callbacks) {
            this.callbacks = callbacks;
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
        public @NotNull Handle<Data> subscribe(final @NotNull Consumer<? super Data> listener) {
            final var handle = new HandleImpl<Data>(listener);
            ListenerDispatcher.this.listenersLock.lock();
            try {
                ListenerDispatcher.this.handles.add(handle);
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
            return handle;
        }

        public void unsubscribe(final @NotNull Handle<? super Data> handle) {
            ListenerDispatcher.this.listenersLock.lock();
            try {
                for (final var myHandle : ListenerDispatcher.this.handles) {
                    if (myHandle.equals(handle)) {
                        Handle.cancel(myHandle);
                        break;
                    }
                }
            } finally {
                ListenerDispatcher.this.listenersLock.unlock();
            }
        }
    }
}
