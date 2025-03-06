package rars.riscv.dump.formats

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import rars.Globals
import rars.assembler.DataTypes
import rars.exceptions.MemoryError
import rars.riscv.hardware.Memory
import rars.settings.BoolSetting
import rars.util.BinaryUtilsOld
import rars.util.toHexStringWithPrefix
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * Dump memory contents in Segment Window format. Each line of
 * text output resembles the Text Segment Window or Data Segment Window
 * depending on which segment is selected for the dump. Written
 * using PrintStream's println() method. Each line of Text Segment
 * Window represents one word of text segment memory. The line
 * includes (1) address, (2) machine code in hex, (3) basic instruction,
 * (4) source line. Each line of Data Segment Window represents 8
 * words of data segment memory. The line includes address of first
 * word for that line followed by 8 32-bit values.
 *
 *
 * In either case, addresses and values are displayed in decimal or
 * hexadecimal representation according to the corresponding settings.
 *
 * @author Pete Sanderson
 * @version January 2008
 */
object SegmentWindowDumpFormat : AbstractDumpFormat(
    "Text/Data Segment Window", "SegmentWindow",
    " Text Segment Window or Data Segment Window format to text file"
) {
    /**
     * {@inheritDoc}
     *
     *
     * Write memory contents in Segment Window format. Each line of
     * text output resembles the Text Segment Window or Data Segment Window
     * depending on which segment is selected for the dump. Written
     * using PrintStream's println() method.
     *
     * @return
     * @see AbstractDumpFormat
     */
    @Throws(IOException::class)
    override fun dumpMemoryRange(
        file: File,
        firstAddress: Int,
        lastAddress: Int,
        memory: Memory
    ): Either<MemoryError, Unit> {
        // TODO: check if these settings work right
        val doDisplayAddressesInHex = Globals.BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX)

        if (Globals.MEMORY_INSTANCE.isAddressInDataSegment(firstAddress))
            return PrintStream(FileOutputStream(file)).doDumpDataSegment(
                firstAddress,
                lastAddress,
                memory,
                doDisplayAddressesInHex
            )
        if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(firstAddress))
            return PrintStream(FileOutputStream(file)).doDumpTextSegment(
                firstAddress,
                lastAddress,
                memory,
                doDisplayAddressesInHex
            )
        error("Address not in text or data segment")
    }

    private fun PrintStream.doDumpDataSegment(
        firstAddress: Int,
        lastAddress: Int,
        memory: Memory,
        doDisplayAddressesInHex: Boolean
    ): Either<MemoryError, Unit> = use { out ->
        TODO()
//        val builder = StringBuilder()
//        var offset = 0
//        for (address in firstAddress..lastAddress step DataTypes.WORD_SIZE) {
//            if (offset % 8 == 0) {
//                val formattedAddress = if (doDisplayAddressesInHex)
//                    address.toHexStringWithPrefix()
//                else
//                    BinaryUtilsOld.unsignedIntToIntString(address)
//                builder.append(formattedAddress).append("    ")
//            }
//            offset++
//            val optWord: Either<MemoryError?, Int?> = memory.getRawWordOrNull(address)
//            if (optWord == null) {
//                break
//            }
//            builder.append(
//                if (doDisplayAddressesInHex)
//                    optWord.toHexStringWithPrefix()
//                else
//                    ("           " + optWord).substring(optWord.toString().length)
//            ).append(" ")
//            if (offset % 8 == 0) {
//                out.println(builder)
//            }
//        }
    }

    private fun PrintStream.doDumpTextSegment(
        firstAddress: Int,
        lastAddress: Int,
        memory: Memory,
        doDisplayAddressesInHex: Boolean
    ): Either<MemoryError, Unit> = use { out ->
        out.println("Address     Code        Basic                        Line Source")
        out.println()
        for (address in firstAddress..lastAddress step DataTypes.WORD_SIZE) memory
            .getRawWordOrNull(address)
            .unwrap { return@use it.left() }
            ?.let { word ->
                buildString {
                    val formattedAddress = if (doDisplayAddressesInHex)
                        address.toHexStringWithPrefix()
                    else
                        BinaryUtilsOld.unsignedIntToIntString(address)
                    append(formattedAddress).append("    ")
                    append(word.toHexStringWithPrefix()).append("  ")
                    memory.getProgramStatement(address).onRight { ps ->
                        append(String.format("%-29s", ps!!.printableBasicAssemblyStatement))
                        if (ps.sourceLine != null) {
                            append(String.format("%-5s", ps.sourceLine.lineNumber))
                            append(ps.sourceLine.source)
                        }
                    }
                }
            }?.let(out::println)
            ?: break
        Unit.right()
    }
}
