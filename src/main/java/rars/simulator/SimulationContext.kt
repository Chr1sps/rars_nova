package rars.simulator

import rars.io.AbstractIO
import rars.riscv.hardware.memory.Memory
import rars.riscv.hardware.registerfiles.CSRegisterFile
import rars.riscv.hardware.registerfiles.FloatingPointRegisterFile
import rars.riscv.hardware.registerfiles.RegisterFile

@JvmRecord
data class SimulationContext(
    @JvmField val registerFile: RegisterFile,
    @JvmField val fpRegisterFile: FloatingPointRegisterFile,
    @JvmField val csrRegisterFile: CSRegisterFile,
    @JvmField val memory: Memory,
    @JvmField val io: AbstractIO
)
