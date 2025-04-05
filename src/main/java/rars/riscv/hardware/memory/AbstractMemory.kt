package rars.riscv.hardware.memory

import arrow.core.Either
import rars.ProgramStatement
import rars.events.MemoryError
import rars.notices.MemoryAccessNotice
import rars.util.Listener

interface ReadableMemory<T> {
    /**
     * The memory configuration for this memory.
     *
     * NOTE: the addresses used in this memory *may not* correspond to
     * actual boundaries of each of the segments.
     */
    val memoryConfiguration: AbstractMemoryConfiguration<T>

    /**
     * Reads specified Memory byte as a byte value.
     *
     * @param address
     * Address of memory byte to be read.
     * @return Byte value stored at that address or [MemoryError]
     * in case of an error.
     */
    fun getByte(address: T): Either<MemoryError, Byte>
    fun getHalf(address: T): Either<MemoryError, Short>
    fun getWord(address: T): Either<MemoryError, Int>
    fun getDoubleWord(address: T): Either<MemoryError, Long>
    fun getProgramStatement(address: T): Either<MemoryError, ProgramStatement?>
}

interface MutableMemory<T> : ReadableMemory<T> {

    fun setByte(address: T, value: Byte): Either<MemoryError, Unit>
    fun setHalf(address: T, value: Short): Either<MemoryError, Unit>
    fun setWord(address: T, value: Int): Either<MemoryError, Unit>
    fun setDoubleWord(address: T, value: Long): Either<MemoryError, Unit>
    fun setProgramStatement(address: T, statement: ProgramStatement): Either<MemoryError, Unit>

    /**
     * Returns the next available word-aligned heap address.
     *
     * @param amount
     * Number of bytes requested. Should be multiple of 4, otherwise
     * next higher multiple of 4 allocated.
     * @return address of allocated heap storage or error message.
     */
    fun allocateBytes(amount: T): Either<String, T>
}

interface SubscribableMemory<T> : MutableMemory<T> {
    /**
     * A view into this memory, whose methods do not trigger the
     * memory access listeners.
     */
    val silentMemoryView: ReadableMemory<T>

    /**
     * Subscribes a listener to memory access events. The listener will
     * be called whenever a memory access occurs within the specified
     * address range. The range is start inclusive, end exclusive.
     *
     * @return a [MemoryListenerHandle] that can be used to unsubscribe
     * from this instance or [MemoryError] in case of an error.
     */
    fun subscribe(
        listener: Listener<MemoryAccessNotice>,
        startAddress: T,
        endAddress: T,
    ): Either<MemoryError, MemoryListenerHandle<T>>

    /**
     * Unsubscribes a listener from memory access events.
     */
    fun unsubscribe(handle: MemoryListenerHandle<T>)
}

fun <T> SubscribableMemory<T>.subscribe(
    listener: Listener<MemoryAccessNotice>,
    singleAddress: T
): Either<MemoryError, MemoryListenerHandle<T>> = subscribe(listener, singleAddress, singleAddress)

data class MemoryListenerHandle<T>(
    internal val listener: Listener<MemoryAccessNotice>,
    val startAddress: T,
    val endAddress: T,
)

