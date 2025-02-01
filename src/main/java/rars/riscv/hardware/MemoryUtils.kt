package rars.riscv.hardware

import rars.assembler.DataTypes
import rars.exceptions.AddressErrorException
import rars.exceptions.ExceptionReason

/**
 * Utility to determine if given address is word-aligned.
 *
 * @param address
 * the address to check
 * @return true if address is word-aligned, false otherwise
 */
fun wordAligned(address: Int): Boolean {
    return (address % DataTypes.WORD_SIZE == 0)
}

@Throws(AddressErrorException::class)
fun checkLoadWordAligned(address: Int) {
    if (!wordAligned(address)) {
        throw AddressErrorException(
            "Load address not aligned to word boundary ",
            ExceptionReason.LOAD_ADDRESS_MISALIGNED, address
        )
    }
}

@Throws(AddressErrorException::class)
fun checkStoreWordAligned(address: Int) {
    if (!wordAligned(address)) {
        throw AddressErrorException(
            "Store address not aligned to word boundary ",
            ExceptionReason.STORE_ADDRESS_MISALIGNED, address
        )
    }
}

