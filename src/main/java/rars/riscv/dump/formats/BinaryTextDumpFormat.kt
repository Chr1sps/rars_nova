package rars.riscv.dump.formats

import arrow.core.raise.either
import rars.assembler.DataTypes.WORD_SIZE
import rars.riscv.hardware.Memory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * Class that represents the "binary text" memory dump format. The output
 * is a text file with one word of memory per line. The word is formatted
 * using '0' and '1' characters, e.g. 01110101110000011111110101010011.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
object BinaryTextDumpFormat :
    AbstractDumpFormat("Binary Text", "BinaryText", "Written as '0' and '1' characters to text file") {
    /**
     * {@inheritDoc}
     *
     *
     * Write memory contents in binary text format. Each line of
     * text contains one memory word written as 32 '0' and '1' characters. Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
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
    ) = PrintStream(FileOutputStream(file)).use { out ->
        either {
            for (address in firstAddress..lastAddress step WORD_SIZE) memory
                .getRawWordOrNull(address)
                .bind()
                ?.let {
                    val result = buildString {
                        val temp = Integer.toBinaryString(it)
                        append("0".repeat(32 - temp.length))
                        append(temp)
                    }
                    out.println(result)
                } ?: break
        }
    }
}
