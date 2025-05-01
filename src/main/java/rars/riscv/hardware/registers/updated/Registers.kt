package rars.riscv.hardware.registers.updated


/**
 * Represents a collection of registers.
 */
interface RegisterFile<T> {
    operator fun get(registerNumber: Int): Register<T>
    operator fun get(registerName: String): Register<T>
}