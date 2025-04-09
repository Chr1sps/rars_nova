package rars.simulator

import rars.io.AbstractIO
import rars.riscv.hardware.memory.Memory
import rars.riscv.hardware.registerfiles.CSRegisterFile
import rars.riscv.hardware.registerfiles.FloatingPointRegisterFile
import rars.riscv.hardware.registerfiles.RegisterFile

data class SimulationContext(
    val registerFile: RegisterFile,
    val fpRegisterFile: FloatingPointRegisterFile,
    val csrRegisterFile: CSRegisterFile,
    val memory: Memory,
    val io: AbstractIO
)
