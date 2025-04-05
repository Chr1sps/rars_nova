package rars.riscv.hardware

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes
import rars.events.EventReason
import rars.events.MemoryError

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

