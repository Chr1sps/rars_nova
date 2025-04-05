package rars.riscv.hardware.memory

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import rars.ProgramStatement
import rars.assembler.DataTypes
import rars.events.EventReason
import rars.events.MemoryError
import rars.notices.MemoryAccessNotice
import rars.util.ListenerDispatcher
import java.util.*

/**
 * Utility to determine if given address is word-aligned.
 *
 * @param address
 * the address to check
 * @return true if address is word-aligned, false otherwise
 */
fun wordAligned(address: Int): Boolean = (address % DataTypes.WORD_SIZE == 0)

fun checkLoadWordAligned(address: Int): Either<MemoryError, Unit> = if (!wordAligned(address)) {
    MemoryError(
        "Load address not aligned to word boundary ",
        EventReason.LOAD_ADDRESS_MISALIGNED, address
    ).left()
} else Unit.right()

fun checkStoreWordAligned(address: Int): Either<MemoryError, Unit> = if (!wordAligned(address)) {
    MemoryError(
        "Store address not aligned to word boundary ",
        EventReason.STORE_ADDRESS_MISALIGNED, address
    ).left()
} else Unit.right()

internal const val BLOCK_LENGTH_WORDS = 1024 // allocated blocksize 1024 ints == 4K bytes
internal const val BLOCK_TABLE_LENGTH = 1024 // Each entry of table points to a block.
internal const val MMIO_TABLE_LENGTH = 16 // Each entry of table points to a 4K block.
internal const val TEXT_BLOCK_LENGTH_WORDS = 1024 // allocated blocksize 1024 ints == 4K bytes
internal const val TEXT_BLOCK_TABLE_LENGTH = 1024 // Each entry of table points to a block.

internal fun isDoubleWordAligned(address: Int): Boolean = (address % (DataTypes.WORD_SIZE + DataTypes.WORD_SIZE) == 0)

/**
 * Returns result of substituting specified byte of source value into specified byte
 * of destination value. Byte positions are 0-1-2-3, listed from most to least
 * significant. No endian issues. This is a private helper method used by get() & set().
 */
internal fun replaceByte(
    sourceValue: Int,
    bytePosInSource: Int,
    destinationValue: Int,
    bytePosInDest: Int
): Int {
    // Set source byte value into destination byte position; set other 24 bits to
    // zeros, and bitwise-OR it with. Set 8 bits in destination byte position to 0's,
    val sourceByte = (sourceValue shr (24 - (bytePosInSource * 8))) and 0xFF
    val clearedDest = destinationValue and (0xFF shl (24 - (bytePosInDest * 8))).inv()
    return (sourceByte shl (24 - (bytePosInDest * 8))) or clearedDest
}

/**
 * Store a program statement at the given address. Address has already been verified as valid.
 */
internal fun storeProgramStatement(
    address: Int,
    statement: ProgramStatement?,
    baseAddress: Int,
    blockTable: Array<Array<ProgramStatement?>?>
) {
    val relative = (address - baseAddress) shr 2 // convert byte address to words
    val block: Int = relative / BLOCK_LENGTH_WORDS
    if (block < TEXT_BLOCK_TABLE_LENGTH) {
        if (blockTable[block] == null) {
            // No instructions are stored in this block, so allocate the block.
            blockTable[block] = arrayOfNulls(BLOCK_LENGTH_WORDS)
        }
        val offset: Int = relative % BLOCK_LENGTH_WORDS
        blockTable[block]!![offset] = statement
    }
}

/**
 * Internal class whose objects will represent an observable-observer pair
 * for a given memory address or range.
 */
internal class MemoryObservable(
    val handle: MemoryListenerHandle<Int>
) : Comparable<MemoryObservable> {
    val dispatcher = ListenerDispatcher<MemoryAccessNotice>()
    val hook = this.dispatcher.hook

    init {
        this.hook.subscribe(handle.listener)
    }

    fun match(address: Int): Boolean =
        address.toUInt() in handle.startAddress.toUInt()..<(handle.endAddress.toUInt() + DataTypes.WORD_SIZE.toUInt())

    /**
     * Useful to have for future refactoring, if it actually becomes worthwhile to sort
     * these or put 'em in a tree (rather than sequential search through list).
     */
    override fun compareTo(other: MemoryObservable): Int =
        compareValuesBy(this, other, { it.handle.startAddress.toUInt() }, { it.handle.endAddress.toUInt() })

    override fun equals(other: Any?): Boolean =
        other is MemoryObservable && handle.startAddress == other.handle.startAddress && handle.endAddress == other.handle.endAddress

    override fun hashCode(): Int = Objects.hash(handle.startAddress, handle.endAddress)
}
