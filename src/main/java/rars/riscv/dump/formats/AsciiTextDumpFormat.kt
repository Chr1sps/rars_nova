package rars.riscv.dump.formats

import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes.WORD_SIZE
import rars.riscv.hardware.memory.Memory
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Class that represents the "ASCII text" memory dump format. Memory contents
 * are interpreted as ASCII codes. The output
 * is a text file with one word of memory per line. The word is formatted
 * to leave three spaces for each character. Non-printing charalcters
 * rendered as period (.) as placeholder. Common escaped characters
 * rendered using backslash and single-character descriptor, e.g. \t for tab.
 *
 * @author Pete Sanderson
 * @version December 2010
 */
object AsciiTextDumpFormat :
    AbstractDumpFormat("ASCII Text", "AsciiText", "Memory contents interpreted as ASCII characters") {
    /**
     * {@inheritDoc}
     *
     *
     * Interpret memory contents as ASCII characters. Each line of
     * text contains one memory word written in ASCII characters. Those
     * corresponding to tab, newline, null, etc are rendered as backslash
     * followed by single-character code, e.g. \t for tab, \0 for null.
     * Non-printing character (control code,
     * values above 127) is rendered as a period (.). Written
     * using PrintStream's println() method.
     * Adapted by Pete Sanderson from code written by Greg Gibeling.
     *
     * @return
     * @see AbstractDumpFormat
     */
    override fun dumpMemoryRange(
        file: File, firstAddress: Int, lastAddress: Int,
        memory: Memory
    ) = PrintStream(FileOutputStream(file)).use { out ->
        for (address in firstAddress..lastAddress step WORD_SIZE) memory
            .getRawWordOrNull(address)
            .unwrap { return@use it.left() }
            ?.let(out::println)
            ?: break
        Unit.right()
    }
}
