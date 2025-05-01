package rars.simulator

import rars.riscv.hardware.memory.SubscribableMemory
import rars.riscv.hardware.registerfiles.RegisterFile

/**
 * Represents a RISC-V machine. Each machine consists of the following parts:
 * - memory, which you can listen to for changes
 * - registers, which you can listen to for changes
 *
 */
interface Machine<AddressType> {
    val memory: SubscribableMemory<AddressType>
    val registers: RegisterFile
}