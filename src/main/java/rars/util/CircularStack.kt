package rars.util

interface CircularStack<T> : Iterable<T> {
    val capacity: Int
    val size: Int

    fun push(value: T)
    fun pop(): T?
    fun peek(): T?
    fun isEmpty(): Boolean = size == 0

    companion object {
        operator fun <T> invoke(capacity: Int): CircularStack<T> = ArrayCircularStack(capacity)
    }
}

class ArrayCircularStack<T>(
    override val capacity: Int
) : CircularStack<T> {
    override var size = 0
        private set

    /**
     * Index of the top element on this stack, **not** the next index after.
     * -1 when the stack is empty.
     */
    private var top = -1
    private val stack: Array<T?> = arrayOfNulls(capacity)

    override fun push(value: T) {
        top = (top + 1) % capacity // also works in the `top == -1` case
        size = (size + 1).coerceAtMost(capacity)
        stack[top] = value
    }

    override fun pop(): T? = if (isEmpty()) null else {
        val result = stack[top]
        stack[top] = null
        top = if (size == 1) -1 else (top + capacity - 1) % capacity
        size--
        result
    }

    override fun peek(): T? = if (isEmpty()) null else stack[top]

    override fun iterator(): Iterator<T> = if (size == 0) EmptyIterator
    else IteratorImpl()

    private inner class IteratorImpl : Iterator<T> {
        private var hasReachedEnd = false
        private var currentIndex: Int = ((top + capacity - size) + 1) % capacity

        override fun next(): T {
            when {
                hasReachedEnd -> throw NoSuchElementException("Iterator has no more elements.")
                currentIndex == top -> hasReachedEnd = true
            }
            return stack[currentIndex]!!.also { currentIndex = (currentIndex + 1) % capacity }
        }

        override fun hasNext(): Boolean = !hasReachedEnd
    }
}