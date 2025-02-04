package rars.riscv.instructions

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import rars.ProgramStatement
import rars.exceptions.BreakpointEvent
import rars.exceptions.ExceptionReason
import rars.exceptions.SimulationError
import rars.exceptions.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstruction.Companion.BASIC_INSTRUCTION_LENGTH
import rars.riscv.BasicInstructionFormat
import rars.riscv.Syscall
import rars.simulator.SimulationContext
import rars.util.ignoreOk

object BasicInstructions {
    private fun basicInstruction(
        example: String,
        description: String,
        format: BasicInstructionFormat,
        operandMask: String,
        callback: SimulationContext.(ProgramStatement) -> Either<SimulationEvent, Unit>
    ): BasicInstruction = object : BasicInstruction(example, description, format, operandMask) {
        override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> =
            callback(statement)
    }

    @JvmField
    val AUIPC = basicInstruction(
        "auipc t1,100000", "Add upper immediate to pc: set t1 to (pc plus an upper 20-bit immediate)",
        BasicInstructionFormat.U_FORMAT, "ssssssssssssssssssss fffff 0010111"
    ) { statement ->
        val shiftedValue = statement.getOperand(1) shl 12
        val convertedValue = shiftedValue.toLong()
        val newValue = registerFile.programCounter - BASIC_INSTRUCTION_LENGTH + convertedValue
        registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val CSRRC = basicInstruction(
        "csrrc t0, fcsr, t1",
        "Atomic Read/Clear CSR: read from the CSR into t0 and clear bits of the CSR according to t1",
        BasicInstructionFormat.I_FORMAT,
        "ssssssssssss ttttt 011 fffff 1110011"
    ) { statement ->
        either {
            val csr = csrRegisterFile.getLongValue(statement.getOperand(1))
            ensureNotNull(csr) {
                SimulationError.create(
                    statement,
                    "Attempt to access unavailable CSR",
                    ExceptionReason.ILLEGAL_INSTRUCTION
                )
            }
            if (statement.getOperand(2) != 0) {
                val previousValue = csrRegisterFile.getLongValue(statement.getOperand(1))
                csrRegisterFile.updateRegisterByNumber(
                    statement.getOperand(1),
                    previousValue!! and registerFile.getLongValue(statement.getOperand(2))!!.inv()
                ).bind()
            }
            registerFile.updateRegisterByNumber(statement.getOperand(0), csr).bind()
        }
    }

    @JvmField
    val EBREAK = basicInstruction(
        "ebreak", "Pause execution",
        BasicInstructionFormat.I_FORMAT, "000000000001 00000 000 00000 1110011"
    ) { BreakpointEvent.left() }

    @JvmField
    val ECALL = basicInstruction(
        "ecall", "Issue a system call : Execute the system call specified by value in a7",
        BasicInstructionFormat.I_FORMAT, "000000000000 00000 000 00000 1110011"
    ) { statement ->
        val number = registerFile.getIntValue("a7")!!
        val syscall = Syscall.findSyscall(number)
        if (syscall != null) {
            val isWriting = when (syscall) {
                Syscall.Close,
                Syscall.PrintDouble,
                Syscall.PrintFloat,
                Syscall.PrintInt,
                Syscall.PrintIntBinary,
                Syscall.PrintIntHex,
                Syscall.PrintIntUnsigned,
                Syscall.PrintString,
                Syscall.Write -> true

                else -> false
            }
            if (!isWriting) io.flush()
            syscall.run { this@basicInstruction.simulate(statement) }
        } else {
            SimulationError.create(
                statement,
                "invalid or unimplemented syscall service: $number",
                ExceptionReason.ENVIRONMENT_CALL
            ).left()
        }
    }
}