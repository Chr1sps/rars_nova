package rars.util

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

typealias Listener<Data> = (Data) -> Unit

class ListenerDispatcher<Data> {
    private val listeners = mutableSetOf<ListenerWrapper<Data>>()
    private val listenersLock: Lock = ReentrantReadWriteLock().writeLock()

    /**
     * Notifies all listeners with the given data.
     *
     * @param data
     * The data to pass to the listeners.
     */
    fun dispatch(data: Data) {
        this.listenersLock.lock()
        try {
            for (listener in this.listeners) {
                if (!listener.isCancelled) {
                    listener.innerListener(data)
                }
            }
            this.listeners.removeIf { it.isCancelled }
        } finally {
            this.listenersLock.unlock()
        }
    }

    /** A hook that listeners can subscribe to. */
    val hook get() = Hook()

    /**
     * To prevent co-modification errors in the inner set, we wrap the listeners in a class that
     * allows us to mark them as cancelled. The purpose of this is to allow listeners to call
     * [Hook.unsubscribe] in their body to remove themselves from the list of listeners.
     */
    private class ListenerWrapper<Data>(
        val innerListener: Listener<Data>,
        var isCancelled: Boolean = false
    )

    /**
     * Inner class that exposes an entry point for listeners to subscribe to.
     * This is done to separate the publisher API from the listeners.
     */
    inner class Hook internal constructor() {
        /**
         * Adds a listener that will be run when the hook is notified.
         *
         * @param listener
         * The listener to add.
         */
        fun subscribe(listener: Listener<Data>) {
            this@ListenerDispatcher.listenersLock.lock()
            try {
                this@ListenerDispatcher.listeners.add(ListenerWrapper<Data>(listener))
            } finally {
                this@ListenerDispatcher.listenersLock.unlock()
            }
        }

        fun unsubscribe(listener: Listener<Data>) {
            this@ListenerDispatcher.listenersLock.lock()
            try {
                for (wrapper in this@ListenerDispatcher.listeners) {
                    if (wrapper.innerListener == listener) {
                        wrapper.isCancelled = true
                        break
                    }
                }
            } finally {
                this@ListenerDispatcher.listenersLock.unlock()
            }
        }
    }
}
