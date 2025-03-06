package rars.riscv.dump.formats

import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes.WORD_SIZE
import rars.riscv.hardware.Memory
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * Class that represents the "binary" memory dump format. The output
 * is a binary file containing the memory words as a byte stream. Output
 * is produced using PrintStream's write() method.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
object BinaryDumpFormat : AbstractDumpFormat("Binary", "Binary", "Written as byte stream to binary file") {
    /**
     * {@inheritDoc}
     *
     *
     * Write memory contents in pure binary format. One byte at a time
     * using PrintStream's write() method. Adapted by Pete Sanderson from
     * code written by Greg Gibeling.
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
        for (address in firstAddress..lastAddress step WORD_SIZE) memory
            .getRawWordOrNull(address)
            .unwrap { return@use it.left() }
            ?.let {
                for (i in 0..3) {
                    out.write((it ushr (i shl 3)) and 0xFF)
                }
            }?.let(out::println)
            ?: break
        Unit.right()
    }
}
