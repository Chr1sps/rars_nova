package rars.riscv.hardware

import arrow.core.Either
import rars.ProgramStatement
import rars.exceptions.MemoryError
import rars.notices.MemoryAccessNotice
import rars.util.Listener

interface ReadableMemory<T : Comparable<T>> {
    /**
     * The memory configuration for this memory.
     *
     * NOTE: the addresses used in this memory *may not* correspond to
     * actual boundaries of each of the segments.
     */
    val memoryConfiguration: MemoryConfiguration

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

interface MutableMemory<T : Comparable<T>> : ReadableMemory<T> {

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

interface SubscribableMemory<T : Comparable<T>> : MutableMemory<T> {
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
): Either<MemoryError, MemoryListenerHandle<T>>
        where
        T : Number,
        T : Comparable<T> = subscribe(listener, singleAddress, singleAddress)

data class MemoryListenerHandle<T>(
    internal val listener: Listener<MemoryAccessNotice>,
    val startAddress: T,
    val endAddress: T,
)

data class MemoryConfigurationNew<T : Comparable<T>>(
    val identifier: String,
    val description: String,

    val textSegmentRange: ClosedRange<T>,
    val dataSegmentRange: ClosedRange<T>,
    val mmioAddressRange: ClosedRange<T>,
    val kernelAddressRange: ClosedRange<T>,
    val heapStackAddressRange: ClosedRange<T>,

    val externAddress: T,
    val globalPointerAddress: T,
    val stackPointerAddress: T,
) {
    init {
        require(heapStackAddressRange in dataSegmentRange)
        require(mmioAddressRange in kernelAddressRange)
    }

    val userHighAddress: T = maxOf(
        textSegmentRange.endInclusive,
        dataSegmentRange.endInclusive,
    )

    companion object {
        val TEXT_BASED_COMPACT_32 = MemoryConfigurationNew<Int>(
            "CompactTextAtZero",
            "Compact, text at address 0",

            textSegmentRange = 0x00000000..0x00000fff,
            dataSegmentRange = 0x00001000..0x00003fff,
            heapStackAddressRange = 0x00003000..0x00003fff,
            kernelAddressRange = 0x00004000..0x00007fff,
            mmioAddressRange = 0x00007f00..0x00007fff,

            externAddress = 0x00001000,
            globalPointerAddress = 0x00001800,
            stackPointerAddress = 0x00003ffc,
        )
    }
}

operator fun <T : Comparable<T>> ClosedRange<T>.contains(otherRange: ClosedRange<T>) =
    otherRange.start >= start && otherRange.endInclusive <= endInclusive