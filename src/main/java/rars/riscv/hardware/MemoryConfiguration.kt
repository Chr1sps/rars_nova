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
    /** starting address for stack  */
    @JvmField val stackPointerAddress: Int,
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
        0x00400000,  // .text Base Address
        0x10000000,  // Data Segment base address
        0x10000000,  // .extern Base Address
        0x10008000,  // Global Pointer $gp)
        0x10010000,  // .data base Address
        0x10040000,  // heap base address
        0x7fffeffc,  // stack pointer $sp (from SPIM not MIPS)
        0x7ffffffc,  // stack base address
        0x7fffffff,  // highest address in user space
        -0x80000000,  // lowest address in kernel space
        -0x10000,  // MMIO base address
        -0x1,  // highest address in kernel (and memory)
        0x7fffffff,  // data segment limit address
        0x0ffffffc,  // text limit address
        0x10040000,  // stack limit address
        -0x1 // memory map limit address
    ),
    DATA_BASED_COMPACT(
        "CompactDataAtZero",
        "Compact, data at address 0",
        0x00003000,  // .text Base Address
        0x00000000,  // Data Segment base address
        0x00001000,  // .extern Base Address
        0x00001800,  // Global Pointer $gp)
        0x00000000,  // .data base Address
        0x00002000,  // heap base address
        0x00002ffc,  // stack pointer $sp
        0x00002ffc,  // stack base address
        0x00003fff,  // highest address in user space
        0x00004000,  // lowest address in kernel space
        0x00007f00,  // MMIO base address
        0x00007fff,  // highest address in kernel (and memory)
        0x00002fff,  // data segment limit address
        0x00003ffc,  // text limit address
        0x00002000,  // stack limit address
        0x00007fff // memory map limit address
    ),
    TEXT_BASED_COMPACT(
        "CompactTextAtZero",
        "Compact, text at address 0",
        0x00000000,  // .text Base Address
        0x00001000,  // Data Segment base address
        0x00001000,  // .extern Base Address
        0x00001800,  // Global Pointer $gp)
        0x00002000,  // .data base Address
        0x00003000,  // heap base address
        0x00003ffc,  // stack pointer $sp
        0x00003ffc,  // stack base address
        0x00003fff,  // highest address in user space
        0x00004000,  // lowest address in kernel space
        0x00007f00,  // MMIO base address
        0x00007fff,  // highest address in kernel (and memory)
        0x00003fff,  // data segment limit address
        0x00000ffc,  // text limit address
        0x00003000,  // stack limit address
        0x00007fff // memory map limit address
    );

    override fun toString() = identifier

    companion object {
        @JvmStatic
        fun fromIdString(id: String) = entries.find { it.identifier == id }
    }
}
