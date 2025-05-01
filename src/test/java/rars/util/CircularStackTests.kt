package rars.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CircularStackTests {
    @Test
    fun `empty stack`() {
        val stack = CircularStack<Int>(10)
        assertEquals(10, stack.capacity)
        assertEquals(0, stack.size)
        assertEquals(null, stack.peek())
        assertEquals(null, stack.pop())
    }

    @Test
    fun `iterator works`() {
        val elements = arrayOf(1, 2, 3, 4, 5)
        val stack = circularStackOf(*elements, capacity = 10)
        stack.assertIterator(*elements)
    }

    @Test
    fun `wrapping around`() {
        val stack = buildCircularStack(capacity = 5) {
            for (i in 0..6) push(i)
        }
        assertEquals(5, stack.size)
        assertEquals(5, stack.capacity)
        assertEquals(6, stack.peek())
        stack.assertIterator(2, 3, 4, 5, 6)
    }
}

private fun <T> circularStackOf(vararg elements: T, capacity: Int): CircularStack<T> {
    require(capacity >= elements.size) { "Capacity must be greater than or equal to the number of elements." }
    return CircularStack<T>(capacity).apply {
        for (element in elements) push(element)
    }
}

private fun <T> buildCircularStack(capacity: Int, builder: CircularStack<T>.() -> Unit): CircularStack<T> =
    CircularStack<T>(capacity).apply(builder)

private fun <T> Iterable<T>.assertIterator(vararg elements: T) {
    val actual = this.iterator()
    val expected = elements.iterator()
    var iteratorLength = 0
    while (true) {
        assertEquals(expected.hasNext(), actual.hasNext()) {
            "Iterator length mismatch. Expected ${elements.size}, got $iteratorLength."
        }
        iteratorLength++
        if (!actual.hasNext()) break
        val expectedElement = expected.next()
        val actualElement = actual.next()
        assertEquals(expectedElement, actualElement) {
            "Unexpected element at index $iteratorLength."
        }
    }
}