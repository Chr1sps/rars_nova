package rars.riscv.dump.formats

import arrow.core.left
import arrow.core.right
import rars.assembler.DataTypes
import rars.riscv.hardware.memory.Memory
import rars.util.unwrap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.util.*

/**
 * Intel's Hex memory initialization format
 *
 * @author Leo Alterman
 * @version July 2011
 */
object IntelHexDumpFormat : AbstractDumpFormat("Intel hex format", "HEX", "Written as Intel Hex Memory File") {
    /**
     * {@inheritDoc}
     *
     * Write memory contents according to the Memory Initialization File
     * (MIF) specification.
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
        for (address in firstAddress..lastAddress step DataTypes.WORD_SIZE) {
            val word = memory
                .getRawWordOrNull(address)
                .unwrap { return@use it.left() }
                ?: break
            val string = buildString {
                val temp = Integer.toHexString(word)
                repeat(8 - temp.length) { append('0') }
                append(temp)
            }
            val addressString = buildString {
                val temp = Integer.toHexString(address - firstAddress)
                repeat(4 - temp.length) { append('0') }
                append(temp)
            }
            val checkSum = run {
                var temp = 0
                temp += 4
                temp += 0xFF and (address - firstAddress)
                temp += 0xFF and ((address - firstAddress) shr 8)
                temp += 0xFF and word
                temp += 0xFF and (word shr 8)
                temp += 0xFF and (word shr 16)
                temp += 0xFF and (word shr 24)
                temp = temp % 256
                temp = temp.inv() + 1
                val tempString = Integer.toHexString(0xFF and temp)
                if (tempString.length == 1) "0$tempString" else tempString
            }
            val finalString = ":04" + addressString + "00" + string + checkSum
            out.println(finalString.uppercase(Locale.getDefault()))
        }
        out.println(":00000001FF")
        Unit.right()
    }
}
