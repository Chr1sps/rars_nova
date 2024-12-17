package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MemoryConfiguration {
    DEFAULT(
            "Default",
            "Default",
            0x00400000, // .text Base Address
            0x10000000, // Data Segment base address
            0x10000000, // .extern Base Address
            0x10008000, // Global Pointer $gp)
            0x10010000, // .data base Address
            0x10040000, // heap base address
            0x7fffeffc, // stack pointer $sp (from SPIM not MIPS)
            0x7ffffffc, // stack base address
            0x7fffffff, // highest address in user space
            0x80000000, // lowest address in kernel space
            0xffff0000, // MMIO base address
            0xffffffff, // highest address in kernel (and memory)
            0x7fffffff, // data segment limit address
            0x0ffffffc, // text limit address
            0x10040000, // stack limit address
            0xffffffff // memory map limit address
    ),
    DATA_BASED_COMPACT(
            "CompactDataAtZero",
            "Compact, data at address 0",
            0x00003000, // .text Base Address
            0x00000000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00000000, // .data base Address
            0x00002000, // heap base address
            0x00002ffc, // stack pointer $sp
            0x00002ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00002fff, // data segment limit address
            0x00003ffc, // text limit address
            0x00002000, // stack limit address
            0x00007fff // memory map limit address
    ),
    TEXT_BASED_COMPACT(
            "CompactTextAtZero",
            "Compact, text at address 0",
            0x00000000, // .text Base Address
            0x00001000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00002000, // .data base Address
            0x00003000, // heap base address
            0x00003ffc, // stack pointer $sp
            0x00003ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00003fff, // data segment limit address
            0x00000ffc, // text limit address
            0x00003000, // stack limit address
            0x00007fff // memory map limit address
    );

    public final @NotNull String identifier, description;
    public final int textBaseAddress;
    public final int dataSegmentBaseAddress;
    public final int externBaseAddress;
    public final int globalPointerAddress;
    public final int dataBaseAddress;
    public final int heapBaseAddress;
    public final int stackPointerAddress;
    public final int stackBaseAddress;
    public final int userHighAddress;
    public final int kernelBaseAddress;
    public final int memoryMapBaseAddress;
    public final int kernelHighAddress;
    public final int dataSegmentLimitAddress;
    public final int textLimitAddress;
    public final int stackLimitAddress;
    public final int memoryMapLimitAddress;
    MemoryConfiguration(
            @NotNull final String identifier,
            @NotNull final String description,
            final int textBaseAddress,
            final int dataSegmentBaseAddress,
            final int externBaseAddress,
            final int globalPointerAddress,
            final int dataBaseAddress,
            final int heapBaseAddress,
            final int stackPointerAddress,
            final int stackBaseAddress,
            final int userHighAddress,
            final int kernelBaseAddress,
            final int memoryMapBaseAddress,
            final int kernelHighAddress,
            final int dataSegmentLimitAddress,
            final int textLimitAddress,
            final int stackLimitAddress,
            final int memoryMapLimitAddress) {
        this.identifier = identifier;
        this.description = description;
        this.textBaseAddress = textBaseAddress;
        this.dataSegmentBaseAddress = dataSegmentBaseAddress;
        this.externBaseAddress = externBaseAddress;
        this.globalPointerAddress = globalPointerAddress;
        this.dataBaseAddress = dataBaseAddress;
        this.heapBaseAddress = heapBaseAddress;
        this.stackPointerAddress = stackPointerAddress;
        this.stackBaseAddress = stackBaseAddress;
        this.userHighAddress = userHighAddress;
        this.kernelBaseAddress = kernelBaseAddress;
        this.memoryMapBaseAddress = memoryMapBaseAddress;
        this.kernelHighAddress = kernelHighAddress;
        this.dataSegmentLimitAddress = dataSegmentLimitAddress;
        this.textLimitAddress = textLimitAddress;
        this.stackLimitAddress = stackLimitAddress;
        this.memoryMapLimitAddress = memoryMapLimitAddress;
    }

    public static @Nullable MemoryConfiguration fromIdString(final @NotNull String id) {
        for (final MemoryConfiguration configuration : MemoryConfiguration.values()) {
            if (configuration.identifier.equals(id)) {
                return configuration;
            }
        }
        return null;
    }
}
