package rars.simulator

import rars.io.AbstractIO
import rars.riscv.hardware.Memory
import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile
import rars.riscv.hardware.registerFiles.RegisterFile

@JvmRecord
data class SimulationContext(
    @JvmField val registerFile: RegisterFile,
    @JvmField val fpRegisterFile: FloatingPointRegisterFile,
    @JvmField val csrRegisterFile: CSRegisterFile,
    @JvmField val memory: Memory,
    @JvmField val io: AbstractIO
)
