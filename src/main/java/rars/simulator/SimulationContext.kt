package rars.simulator

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import rars.ErrorMessage
import rars.ProgramStatement
import rars.events.EventReason
import rars.events.ExitingError
import rars.events.MemoryError
import rars.events.SimulationError
import rars.ieee754.RoundingMode
import rars.io.AbstractIO
import rars.riscv.BasicInstruction
import rars.riscv.hardware.memory.Memory
import rars.riscv.hardware.registerfiles.CSRegisterFile
import rars.riscv.hardware.registerfiles.FloatingPointRegisterFile
import rars.riscv.hardware.registerfiles.RegisterFile
import rars.util.toHexStringWithPrefix

data class SimulationContext(
    val registerFile: RegisterFile,
    val fpRegisterFile: FloatingPointRegisterFile,
    val csrRegisterFile: CSRegisterFile,
    val memory: Memory,
    val io: AbstractIO
) {
    fun SimulationError.Companion.create(
        statement: ProgramStatement,
        message: String,
        reason: EventReason
    ): SimulationError {
        val address =
            registerFile.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
        return create(
            reason,
            ErrorMessage.error(
                statement.sourceProgram,
                statement.sourceLine!!.lineNumber,
                0,
                "Runtime exception at ${address.toHexStringWithPrefix()}: $message"
            ),
            0
        )
    }

    fun SimulationError.Companion.create(
        statement: ProgramStatement,
        memoryError: MemoryError
    ): SimulationError {
        val address =
            registerFile.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
        return create(
            memoryError.reason,
            ErrorMessage.error(
                statement.sourceProgram,
                statement.sourceLine!!.lineNumber,
                0,
                "Runtime exception at ${address.toHexStringWithPrefix()}: ${memoryError.message}"
            ),
            memoryError.address
        )
    }

    fun CSRegisterFile.getRoundingMode(
        rmValue: Int,
        statement: ProgramStatement
    ): Either<SimulationError, RoundingMode> {
        val frm = getInt("frm")!!
        val rm = if (rmValue == 7) {
            frm
        } else {
            rmValue
        }
        return when (rm) {
            0 -> RoundingMode.EVEN.right() // RNE
            1 -> RoundingMode.ZERO.right() // RTZ
            2 -> RoundingMode.MIN.right()  // RDN
            3 -> RoundingMode.MAX.right()  // RUP
            4 -> RoundingMode.AWAY.right() // RMM
            else -> SimulationError.create(
                statement,
                "Invalid rounding mode. RM = $rmValue and frm = $frm",
                EventReason.OTHER
            ).left()
        }
    }

    operator fun ExitingError.Companion.invoke(
        statement: ProgramStatement,
        memoryError: MemoryError,
    ): ExitingError {
        val address =
            (registerFile.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH).toHexStringWithPrefix()
        return ExitingError(
            memoryError.reason,
            ErrorMessage.error(
                statement.sourceProgram,
                statement.sourceLine!!.lineNumber,
                0,
                "Runtime exception at $address: ${memoryError.message}"
            ),
            memoryError.address
        )
    }

}
