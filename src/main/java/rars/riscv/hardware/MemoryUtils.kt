package rars.riscv.hardware;

import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExceptionReason;

public enum MemoryUtils {
    ;

    /**
     * Utility to determine if given address is word-aligned.
     *
     * @param address
     *     the address to check
     * @return true if address is word-aligned, false otherwise
     */
    public static boolean wordAligned(final int address) {
        return (address % DataTypes.WORD_SIZE == 0);
    }

    public static void checkLoadWordAligned(final int address) throws AddressErrorException {
        if (!wordAligned(address)) {
            throw new AddressErrorException(
                "Load address not aligned to word boundary ",
                ExceptionReason.LOAD_ADDRESS_MISALIGNED, address
            );
        }
    }

    public static void checkStoreWordAligned(final int address) throws AddressErrorException {
        if (!wordAligned(address)) {
            throw new AddressErrorException(
                "Store address not aligned to word boundary ",
                ExceptionReason.STORE_ADDRESS_MISALIGNED, address
            );
        }
    }
}
