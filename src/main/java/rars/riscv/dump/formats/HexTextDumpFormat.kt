package rars.riscv.dump.formats

import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes.WORD_SIZE
import rars.riscv.hardware.memory.Memory
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * Class that represents the "hexadecimal text" memory dump format. The output
 * is a text file with one word of memory per line. The word is formatted
 * using hexadecimal characters, e.g. 3F205A39.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
object HexTextDumpFormat : AbstractDumpFormat("Hexadecimal Text", "HexText", "Written as hex characters to text file") {
    /**
     * {@inheritDoc}
     *
     *
     * Write memory contents in hexadecimal text format. Each line of
     * text contains one memory word written in hexadecimal characters. Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
     *
     * @return
     * @see AbstractDumpFormat
     */
    @Throws(IOException::class)
    override fun dumpMemoryRange(
        file: File, firstAddress: Int, lastAddress: Int,
        memory: Memory
    ) = PrintStream(FileOutputStream(file)).use { out ->
        for (address in firstAddress..lastAddress step WORD_SIZE) memory
            .getRawWordOrNull(address)
            .unwrap { return@use it.left() }
            ?.let {
                val hexString = Integer.toHexString(it)
                repeat(8 - hexString.length) { out.print('0') }
                out.println(hexString)
            } ?: break
        Unit.right()
    }
}
