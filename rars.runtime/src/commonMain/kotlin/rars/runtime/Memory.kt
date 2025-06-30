package rars.runtime

import arrow.core.Either

interface ReadableMemory<T> {
    /**
     * The memory configuration for this memory.
     *
     * NOTE: the addresses used in this memory *may not* correspond to
     * actual boundaries of each of the segments.
     */
    val memoryConfiguration: MemoryConfiguration<T>

    /**
     * Reads specified Memory byte as a byte value.
     *
     * @param address
     * Address of memory byte to be read.
     * @return Byte value stored at that address or [MemoryError]
     * in case of an error.
     */
    fun getByte(address: T): Either<MemoryError<T>, Byte>
    fun getHalf(address: T): Either<MemoryError<T>, Short>
    fun getWord(address: T): Either<MemoryError<T>, Int>
    fun getDoubleWord(address: T): Either<MemoryError<T>, Long>
}
