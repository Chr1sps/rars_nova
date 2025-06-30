package rars.util

/**
 * A closed range implementation for types that do not implement [Comparable].
 */
interface MemoryRange<T> {
    val start: T
    val endInclusive: T
    operator fun contains(value: T): Boolean
    fun isEmpty(): Boolean

    companion object {
        fun <T : Comparable<T>> fromClosedRange(
            range: ClosedRange<T>,
        ): MemoryRange<T> = KtClosedRangeDecorator(range)
    }

    private data class KtClosedRangeDecorator<T : Comparable<T>>(private val original: ClosedRange<T>) :
        MemoryRange<T> {
        override val start: T by original::start
        override val endInclusive: T by original::endInclusive
        override fun contains(value: T): Boolean = original.contains(value)
        override fun isEmpty(): Boolean = original.isEmpty()
    }
}

operator fun <T> MemoryRange<T>.contains(otherRange: MemoryRange<T>): Boolean =
    otherRange.start in this && otherRange.endInclusive in this