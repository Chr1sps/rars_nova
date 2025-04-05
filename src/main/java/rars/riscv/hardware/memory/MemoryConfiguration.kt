package rars.riscv.hardware.memory

import rars.util.MyClosedRange


/**
 * Represents a memory configuration for a RISC-V simulator.
 * This interface is generic to facilitate implementing it for
 * different bit-width architectures (e.g., 32-bit, 64-bit).
 *
 * The implementations of this interface *must* satisfy the following
 * conditions:
 * 1. All ranges must be non-empty and end inclusive.
 * 2. Alignment: start values must be aligned to the word boundary, while
 *    end inclusive values must be aligned to the byte boundary (they
 *    represent the last byte address that is contained within them).
 * 3. The range of the heap and stack address must be within the data segment.
 * 4. The MMIO address range must be within the kernel address range.
 * 5. The [userHighAddress] value must be the maximum of either of the
 *    text segment and data segment end addresses.
 *
 * All of these conditions are checked with regard to the assumed ordering
 * of this type.
 */
interface AbstractMemoryConfiguration<T> {
    val identifier: String
    val description: String

    val textSegmentRange: MyClosedRange<T>
    val dataSegmentRange: MyClosedRange<T>
    val mmioAddressRange: MyClosedRange<T>
    val kernelAddressRange: MyClosedRange<T>
    val heapStackAddressRange: MyClosedRange<T>

    val externAddress: T
    val globalPointerAddress: T
    val stackPointerAddress: T
    val userHighAddress: T

    // TODO: investigate the difference between this and the data segment limit addresses
    val dataBaseAddress: T
}

val <T> AbstractMemoryConfiguration<T>.textSegmentBaseAddress get() = textSegmentRange.start
val <T> AbstractMemoryConfiguration<T>.textSegmentLimitAddress get() = textSegmentRange.endInclusive

val <T> AbstractMemoryConfiguration<T>.dataSegmentBaseAddress get() = dataSegmentRange.start
val <T> AbstractMemoryConfiguration<T>.dataSegmentLimitAddress get() = dataSegmentRange.endInclusive

val <T> AbstractMemoryConfiguration<T>.heapBaseAddress get() = this.heapStackAddressRange.start
val <T> AbstractMemoryConfiguration<T>.stackBaseAddress get() = this.heapStackAddressRange.endInclusive

val <T> AbstractMemoryConfiguration<T>.memoryMapBaseAddress get() = this.mmioAddressRange.start
val <T> AbstractMemoryConfiguration<T>.memoryMapLimitAddress get() = this.mmioAddressRange.endInclusive

val <T> AbstractMemoryConfiguration<T>.kernelBaseAddress get() = this.kernelAddressRange.start
val <T> AbstractMemoryConfiguration<T>.kernelHighAddress get() = this.kernelAddressRange.endInclusive

enum class MemoryConfiguration(
    override val identifier: String,
    override val description: String,

    override val textSegmentRange: MyClosedRange<Int>,
    override val dataSegmentRange: MyClosedRange<Int>,
    override val heapStackAddressRange: MyClosedRange<Int>,
    override val kernelAddressRange: MyClosedRange<Int>,
    override val mmioAddressRange: MyClosedRange<Int>,

    override val dataBaseAddress: Int,
    override val externAddress: Int,
    override val globalPointerAddress: Int,
) : AbstractMemoryConfiguration<Int> {
    DEFAULT(
        "Default",
        "Default",
        textSegmentRange = IntMemoryRange(0x00400000, 0x0fffffff),
        dataSegmentRange = IntMemoryRange(0x10000000, 0x7fffffff),

        heapStackAddressRange = IntMemoryRange(0x10040000, 0x7fffffff),

        kernelAddressRange = IntMemoryRange(-0x80000000, -0x00000001),
        mmioAddressRange = IntMemoryRange(-0x00010000, -0x00000001),

        externAddress = 0x10000000,
        globalPointerAddress = 0x10008000,
        dataBaseAddress = 0x10010000,
    ),
    DATA_BASED_COMPACT(
        "CompactDataAtZero",
        "Compact, data at address 0",

        dataSegmentRange = IntMemoryRange(0x00000000, 0x00002fff),
        heapStackAddressRange = IntMemoryRange(0x00002000, 0x00002fff),
        textSegmentRange = IntMemoryRange(0x00003000, 0x00003fff),

        kernelAddressRange = IntMemoryRange(0x00004000, 0x00007fff),
        mmioAddressRange = IntMemoryRange(0x00007f00, 0x00007fff),

        externAddress = 0x00001000,
        globalPointerAddress = 0x00001800,
        dataBaseAddress = 0x00000000,
    ),
    TEXT_BASED_COMPACT(
        "CompactTextAtZero",
        "Compact, text at address 0",

        textSegmentRange = IntMemoryRange(0x00000000, 0x00000fff),
        dataSegmentRange = IntMemoryRange(0x00001000, 0x00003fff),

        heapStackAddressRange = IntMemoryRange(0x00003000, 0x00003fff),

        kernelAddressRange = IntMemoryRange(0x00004000, 0x00007fff),
        mmioAddressRange = IntMemoryRange(0x00007f00, 0x00007fff),

        externAddress = 0x00001000,
        globalPointerAddress = 0x00001800,
        dataBaseAddress = 0x00002000,
    );

    override val userHighAddress: Int = maxOf(
        textSegmentRange.endInclusive,
        dataSegmentRange.endInclusive,
        comparator = Comparator { first, other ->
            first.toUInt().compareTo(other.toUInt())
        }
    )

    override val stackPointerAddress: Int = stackBaseAddress.alignTo(4)

    override fun toString() = identifier

    companion object {
        @JvmStatic
        fun fromIdString(id: String) = entries.find { it.identifier == id }
    }
}

infix fun Int.alignTo(alignment: Int) = this / alignment * alignment
infix fun Long.alignTo(alignment: Long) = this / alignment * alignment
infix fun Short.alignTo(alignment: Short) = this / alignment * alignment
infix fun Byte.alignTo(alignment: Byte) = this / alignment * alignment

data class IntMemoryRange(
    override val start: Int,
    override val endInclusive: Int,
) : MyClosedRange<Int> {
    override fun contains(value: Int): Boolean = value.toUInt() in start.toUInt()..endInclusive.toUInt()
    override fun isEmpty(): Boolean = start.toUInt() > endInclusive.toUInt()
}
