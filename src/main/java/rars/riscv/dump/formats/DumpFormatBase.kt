package rars.riscv.dump.formats

import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes.WORD_SIZE
import rars.riscv.dump.DumpFormat
import rars.riscv.hardware.memory.Memory
import rars.util.toAscii
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class SimpleDumpFormat(
    override val name: String,
    commandDescriptor: String,
    override val description: String,
    val addressDumpCallback: StringBuilder.(Int) -> Unit,
) : DumpFormat {
    override val commandDescriptor: String = commandDescriptor.replace(" ".toRegex(), "")

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
                buildString { addressDumpCallback(it) }
            }?.let(out::println)
            ?: break
        Unit.right()
    }
}

val ASCII_TEXT_DUMP_FORMAT = SimpleDumpFormat(
    "ASCII Text",
    "AsciiText",
    "Memory contents interpreted as ASCII characters"
) { append(it.toAscii()) }

val BINARY_DUMP_FORMAT = SimpleDumpFormat(
    "Binary", "Binary", "Written as byte stream to binary file"
) { for (i in 0..3) append((it ushr (i shl 3)) and 0xFF) }

val BINARY_TEXT_DUMP_FORMAT = SimpleDumpFormat(
    "Binary Text", "BinaryText", "Written as '0' and '1' characters to text file"
) {
    val temp = Integer.toBinaryString(it)
    append("0".repeat(32 - temp.length))
    append(temp)
}

val HEX_TEXT_DUMP_FORMAT = SimpleDumpFormat(
    "Hexadecimal Text", "HexText", "Written as hex characters to text file"
) {
    val hexString = Integer.toHexString(it)
    repeat(8 - hexString.length) { append('0') }
    append(hexString)
}