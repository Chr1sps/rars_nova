package rars.riscv.hardware

enum class MemoryConfiguration(
    val identifier: String,
    @JvmField val description: String,
    /** base address for (user) text segment  */
    @JvmField val textBaseAddress: Int,
    /** base address for (user) data segment  */
    @JvmField val dataSegmentBaseAddress: Int,
    /** base address for .extern directive  */
    @JvmField val externBaseAddress: Int,
    /** base address for storing globals  */
    @JvmField val globalPointerAddress: Int,
    /** base address for storage of non-global static data in data segment  */
    @JvmField val dataBaseAddress: Int,
    /** base address for heap  */
    @JvmField val heapBaseAddress: Int,
    /** base address for stack  */
    @JvmField val stackBaseAddress: Int,
    /** highest address accessible in user (not kernel) mode.  */
    @JvmField val userHighAddress: Int,
    /** kernel boundary. Only OS can access this or higher address  */
    @JvmField val kernelBaseAddress: Int,
    /** starting address for memory mapped I/O  */
    @JvmField val memoryMapBaseAddress: Int,
    /** highest address acessible in kernel mode.  */
    @JvmField val kernelHighAddress: Int,
    @JvmField val dataSegmentLimitAddress: Int,
    @JvmField val textLimitAddress: Int,
    @JvmField val stackLimitAddress: Int,
    @JvmField val memoryMapLimitAddress: Int
) {
    DEFAULT(
        "Default",
        "Default",
        textBaseAddress = 0x00400000,
        textLimitAddress = 0x0ffffffc,

        dataSegmentBaseAddress = 0x10000000,
        externBaseAddress = 0x10000000,

        globalPointerAddress = 0x10008000,
        dataBaseAddress = 0x10010000,

        heapBaseAddress = 0x10040000,
        stackLimitAddress = 0x10040000,
        stackBaseAddress = 0x7ffffffc,

        userHighAddress = 0x7fffffff,
        dataSegmentLimitAddress = 0x7fffffff,

        kernelBaseAddress = -0x80000000,
        memoryMapBaseAddress = -0x00010000,
        memoryMapLimitAddress = -0x00000001,
        kernelHighAddress = -0x00000001,
    ),
    DATA_BASED_COMPACT(
        "CompactDataAtZero",
        "Compact, data at address 0",

        dataSegmentBaseAddress = 0x00000000,
        externBaseAddress = 0x00001000,

        globalPointerAddress = 0x00001800,
        dataBaseAddress = 0x00000000,
        heapBaseAddress = 0x00002000,
        stackBaseAddress = 0x00002ffc,
        dataSegmentLimitAddress = 0x00002fff,
        stackLimitAddress = 0x00002000,

        textBaseAddress = 0x00003000,
        textLimitAddress = 0x00003ffc,
        userHighAddress = 0x00003fff,

        kernelBaseAddress = 0x00004000,
        memoryMapBaseAddress = 0x00007f00,
        memoryMapLimitAddress = 0x00007fff,
        kernelHighAddress = 0x00007fff,
    ),
    TEXT_BASED_COMPACT(
        "CompactTextAtZero",
        "Compact, text at address 0",

        textBaseAddress = 0x00000000,
        textLimitAddress = 0x00000ffc,

        dataSegmentBaseAddress = 0x00001000,
        externBaseAddress = 0x00001000,

        globalPointerAddress = 0x00001800,
        dataBaseAddress = 0x00002000,

        heapBaseAddress = 0x00003000,
        stackLimitAddress = 0x00003000,
        stackBaseAddress = 0x00003ffc,

        userHighAddress = 0x00003fff,
        dataSegmentLimitAddress = 0x00003fff,

        kernelBaseAddress = 0x00004000,
        memoryMapBaseAddress = 0x00007f00,
        memoryMapLimitAddress = 0x00007fff,
        kernelHighAddress = 0x00007fff,
    );

    @JvmField
    val stackPointerAddress: Int = stackBaseAddress.alignTo(4)

    override fun toString() = identifier

    companion object {
        @JvmStatic
        fun fromIdString(id: String) = entries.find { it.identifier == id }
    }
}

/**
 * Represents a memory configuration of the given memory object.
 */

infix fun Int.alignTo(alignment: Int) = this / alignment * alignment
infix fun Long.alignTo(alignment: Long) = this / alignment * alignment
infix fun Short.alignTo(alignment: Short) = this / alignment * alignment
infix fun Byte.alignTo(alignment: Byte) = this / alignment * alignment

//infix fun 