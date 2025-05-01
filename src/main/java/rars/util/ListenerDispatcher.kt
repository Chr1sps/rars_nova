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
                this@ListenerDispatcher.listeners.add(ListenerWrapper(listener))
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


data class SubscriptionHandle<Data>(internal val listener: Listener<Data>)

interface Subscribable<Data> {
    fun subscribe(listener: Listener<Data>): SubscriptionHandle<Data>
    fun unsubscribe(handle: SubscriptionHandle<Data>)
}

interface SubscriptionManager<Data> {
    fun addListener(listener: Listener<Data>): SubscriptionHandle<Data>
    fun removeListener(handle: SubscriptionHandle<Data>)
    fun dispatch(data: Data)
    fun clearListeners()
    fun hasHandle(handle: SubscriptionHandle<Data>): Boolean
}

class UnsafeSubscriptionManager<Data> : SubscriptionManager<Data> {
    private val handles = mutableSetOf<SubscriptionHandle<Data>>()

    override fun addListener(listener: Listener<Data>): SubscriptionHandle<Data> = SubscriptionHandle(listener).also {
        handles.add(it)
    }

    override fun removeListener(handle: SubscriptionHandle<Data>) {
        handles.remove(handle)
    }

    override fun dispatch(data: Data): Unit = handles.forEach { it.listener(data) }

    override fun clearListeners(): Unit = handles.clear()

    override fun hasHandle(handle: SubscriptionHandle<Data>): Boolean = handle in handles
}

class SynchronizedSubscriptionManager<Data> : SubscriptionManager<Data> {
    private val handles = mutableSetOf<SubscriptionHandle<Data>>()
    private val handlesReadLock: Lock
    private val handlesWriteLock: Lock

    init {
        val readWriteLock = ReentrantReadWriteLock()
        handlesReadLock = readWriteLock.readLock()
        handlesWriteLock = readWriteLock.writeLock()
    }

    override fun addListener(listener: Listener<Data>): SubscriptionHandle<Data> = SubscriptionHandle(listener).also {
        withLock(handlesWriteLock) { handles.add(it) }
    }

    override fun removeListener(handle: SubscriptionHandle<Data>): Unit = withLock(handlesWriteLock) {
        handles.remove(handle)
    }

    override fun dispatch(data: Data): Unit = withLock(handlesReadLock) {
        for (handle in handles) {
            handle.listener(data)
        }
    }


    override fun clearListeners() = withLock(handlesWriteLock) {
        handles.clear()
    }

    override fun hasHandle(handle: SubscriptionHandle<Data>): Boolean = withLock(handlesReadLock) {
        handle in handles
    }
}

private fun <T> withLock(lock: Lock, block: () -> T): T {
    lock.lock()
    return try {
        block()
    } finally {
        lock.unlock()
    }
}
