package rars.runtime

import rars.util.MemoryRange

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
interface MemoryConfiguration<T> {
    val identifier: String
    val description: String

    val textSegmentRange: MemoryRange<T>
    val dataSegmentRange: MemoryRange<T>
    val mmioAddressRange: MemoryRange<T>
    val kernelAddressRange: MemoryRange<T>
    val heapStackAddressRange: MemoryRange<T>

    val externAddress: T
    val globalPointerAddress: T
    val stackPointerAddress: T
    val userHighAddress: T

    // TODO: investigate the difference between this and the data segment limit addresses
    val dataBaseAddress: T
}

val <T> MemoryConfiguration<T>.textSegmentBaseAddress get() = textSegmentRange.start
val <T> MemoryConfiguration<T>.textSegmentLimitAddress get() = textSegmentRange.endInclusive

val <T> MemoryConfiguration<T>.dataSegmentBaseAddress get() = dataSegmentRange.start
val <T> MemoryConfiguration<T>.dataSegmentLimitAddress get() = dataSegmentRange.endInclusive

val <T> MemoryConfiguration<T>.heapBaseAddress get() = this.heapStackAddressRange.start
val <T> MemoryConfiguration<T>.stackBaseAddress get() = this.heapStackAddressRange.endInclusive

val <T> MemoryConfiguration<T>.memoryMapBaseAddress get() = this.mmioAddressRange.start
val <T> MemoryConfiguration<T>.memoryMapLimitAddress get() = this.mmioAddressRange.endInclusive

val <T> MemoryConfiguration<T>.kernelBaseAddress get() = this.kernelAddressRange.start
val <T> MemoryConfiguration<T>.kernelHighAddress get() = this.kernelAddressRange.endInclusive