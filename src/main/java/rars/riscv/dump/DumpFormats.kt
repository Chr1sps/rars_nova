package rars.riscv.dump

import rars.riscv.dump.formats.*

/**
 * This class provides a list of all dump formats available in RARS.
 */
object DumpFormats {
    val DUMP_FORMATS = listOf(
        ASCII_TEXT_DUMP_FORMAT,
        BINARY_DUMP_FORMAT,
        BINARY_TEXT_DUMP_FORMAT,
        HEX_TEXT_DUMP_FORMAT,
        IntelHexDumpFormat,
        SegmentWindowDumpFormat
    )

    fun findDumpFormatGivenCommandDescriptor(formatCommandDescriptor: String?): DumpFormat? = DUMP_FORMATS.find {
        it.commandDescriptor == formatCommandDescriptor
    }
}
