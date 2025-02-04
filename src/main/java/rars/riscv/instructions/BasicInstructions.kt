package rars.riscv.instructions

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import rars.ProgramStatement
import rars.exceptions.*
import rars.jsoftfloat.Environment
import rars.jsoftfloat.operations.Arithmetic
import rars.jsoftfloat.types.Float32
import rars.jsoftfloat.types.Float64
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
    ) { stmt ->
        val shiftedValue = stmt.getOperand(1) shl 12
        val convertedValue = shiftedValue.toLong()
        val newValue = registerFile.programCounter - BASIC_INSTRUCTION_LENGTH + convertedValue
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val CSRRC = basicInstruction(
        "csrrc t0, fcsr, t1",
        "Atomic Read/Clear CSR: read from the CSR into t0 and clear bits of the CSR according to t1",
        BasicInstructionFormat.I_FORMAT,
        "ssssssssssss ttttt 011 fffff 1110011"
    ) { stmt ->
        either {
            val csr = csrRegisterFile.getLongValue(stmt.getOperand(1))
            ensureNotNull(csr) {
                SimulationError.create(
                    stmt,
                    "Attempt to access unavailable CSR",
                    ExceptionReason.ILLEGAL_INSTRUCTION
                )
            }
            if (stmt.getOperand(2) != 0) {
                val previousValue = csrRegisterFile.getLongValue(stmt.getOperand(1))
                csrRegisterFile.updateRegisterByNumber(
                    stmt.getOperand(1),
                    previousValue!! and registerFile.getLongValue(stmt.getOperand(2))!!.inv()
                ).bind()
            }
            registerFile.updateRegisterByNumber(stmt.getOperand(0), csr).bind()
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
    ) { stmt ->
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
            syscall.run { this@basicInstruction.simulate(stmt) }
        } else {
            SimulationError.create(
                stmt,
                "invalid or unimplemented syscall service: $number",
                ExceptionReason.ENVIRONMENT_CALL
            ).left()
        }
    }

    @JvmField
    val FSQRTD = basicInstruction(
        "fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
        BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011"
    ) { stmt ->
        either {
            val environment = Environment()
            environment.mode = Floating.FloatingUtils.getRoundingMode(
                stmt.getOperand(2),
                stmt,
                csrRegisterFile
            ).bind()
            val registerValue = fpRegisterFile.getLongValue(stmt.getOperand(1))!!
            val result = Arithmetic.squareRoot(Float64(registerValue), environment)
            Floating.FloatingUtils.setfflags(csrRegisterFile, environment)
            fpRegisterFile.updateRegisterByNumber(stmt.getOperand(0), result.bits)
        }
    }

    @JvmField
    val FSQRTS = basicInstruction(
        "fsqrt.s f1, f2, dyn", "Floating SQuare RooT: Assigns f1 to the square root of f2",
        BasicInstructionFormat.I_FORMAT, "0101100 00000 sssss ttt fffff 1010011"
    ) { stmt ->
        either {
            val environment = Environment()
            environment.mode = Floating.FloatingUtils.getRoundingMode(
                stmt.getOperand(2),
                stmt,
                csrRegisterFile
            ).bind()
            val registerValue = fpRegisterFile.getIntValue(stmt.getOperand(1))!!
            val result = Arithmetic.squareRoot(Float32(registerValue), environment)
            Floating.FloatingUtils.setfflags(csrRegisterFile, environment)
            fpRegisterFile.updateRegisterByNumberInt(stmt.getOperand(0), result.bits)
        }
    }

    @JvmField
    val FSW = basicInstruction(
        "fsw f1, -100(t1)", "Store a float to memory",
        BasicInstructionFormat.S_FORMAT, "sssssss fffff ttttt 010 sssss 0100111"
    ) { stmt ->
        val upperImmediate = (stmt.getOperand(1) shl 20) shr 20
        val address = registerFile.getIntValue(stmt.getOperand(2))!! + upperImmediate
        val value = fpRegisterFile.getLongValue(stmt.getOperand(0))!!.toInt()
        try {
            memory.setWord(address, value)
            Unit.right()
        } catch (e: AddressErrorException) {
            SimulationError.create(stmt, e).left()
        }
    }

    @JvmField
    val JAL = basicInstruction(
        "jal t1, target",
        "Jump and link : Set t1 to Program Counter (return address) then jump to statement at target address",
        BasicInstructionFormat.J_FORMAT,
        "s ssssssssss s ssssssss fffff 1101111 "
    ) { statement ->
        registerFile.updateRegisterByNumber(
            statement.getOperand(0),
            registerFile.programCounter.toLong()
        ).map {
            registerFile.setProgramCounter(
                registerFile.programCounter - BASIC_INSTRUCTION_LENGTH + statement.getOperand(1)
            )
        }
    }

    @JvmField
    val JALR = basicInstruction(
        "jalr t1, t2, -100",
        "Jump and link register: Set t1 to Program Counter (return address) then jump to statement at t2 + " +
                "immediate",
        BasicInstructionFormat.I_FORMAT,
        "tttttttttttt sssss 000 fffff 1100111"
    ) { stmt ->

        val target = registerFile.getIntValue(stmt.getOperand(1))!!
        registerFile.updateRegisterByNumber(
            stmt.getOperand(0),
            registerFile.programCounter.toLong()
        ).map {
            // Set PC = $t2 + immediate with the last bit set to 0
            registerFile.setProgramCounter((target + ((stmt.getOperand(2) shl 20) shr 20)) and -0x2)
        }
    }

    @JvmField
    val LUI = basicInstruction(
        "lui t1,100000",
        "Load upper immediate: set t1 to 20-bit followed by 12 0s",
        BasicInstructionFormat.U_FORMAT,
        "ssssssssssssssssssss fffff 0110111"
    ) { stmt ->
        val shiftedValue = (stmt.getOperand(1) shl 12).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), shiftedValue).ignoreOk()
    }

    @JvmField
    val SLLI32 = basicInstruction(
        "slli t1,t2,10",
        "Shift left logical : Set t1 to result of shifting t2 left by number of bits specified by immediate",
        BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0010011"
    ) { stmt ->
        val newValue = Integer.toUnsignedLong(
            registerFile.getIntValue(stmt.getOperand(1))!!
                    shl stmt.getOperand(2)
        )
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val SLLI64 = basicInstruction(
        "slli t1,t2,33",
        "Shift left logical : Set t1 to result of shifting t2 left by number of bits specified by immediate",
        BasicInstructionFormat.R_FORMAT, "000000 tttttt sssss 001 fffff 0010011"
    ) { stmt ->
        val newValue: Long = registerFile.getLongValue(stmt.getOperand(1))!! shl stmt.getOperand(2)
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val SLLIW = basicInstruction(
        "slliw t1,t2,10",
        "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by " +
                "immediate",
        BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0011011"
    ) { stmt ->
        // Copy from SLLI
        val newValue = (registerFile.getIntValue(stmt.getOperand(1))!! shl stmt.getOperand(
            2
        )).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val SRAI32 = basicInstruction(
        "srai t1,t2,10",
        "Shift right arithmetic : Set t1 to result of sign-extended shifting t2 right by number of bits specified" +
                " by immediate",
        BasicInstructionFormat.R_FORMAT, "0100000 ttttt sssss 101 fffff 0010011"
    ) { stmt ->
        // Uses >> because sign fill
        val newValue = (registerFile.getIntValue(stmt.getOperand(1))!! shr stmt.getOperand(2)).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()

    }

    @JvmField
    val SRAI64 = basicInstruction(
        "srai t1,t2,33",
        "Shift right arithmetic : Set t1 to result of sign-extended shifting t2 right by number of bits specified" +
                " by immediate",
        BasicInstructionFormat.R_FORMAT, "010000 tttttt sssss 101 fffff 0010011"
    ) { stmt ->
        // Uses >> because sign fill
        val shifted = registerFile.getLongValue(stmt.getOperand(1))!! shr stmt.getOperand(2)
        registerFile.updateRegisterByNumber(stmt.getOperand(0), shifted).ignoreOk()
    }

    @JvmField
    val SRAIW = basicInstruction(
        "sraiw t1,t2,10",
        "Shift right arithmetic (32 bit): Set t1 to result of sign-extended shifting t2 right by number of bits " +
                "specified by immediate",
        BasicInstructionFormat.R_FORMAT, "0100000 ttttt sssss 101 fffff 0011011"
    ) { stmt ->
        // Use the code directly from SRAI
        val newValue = (registerFile.getIntValue(stmt.getOperand(1))!! shr stmt.getOperand(2)).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val SRLI32 = basicInstruction(
        "srli t1,t2,10",
        "Shift right logical : Set t1 to result of shifting t2 right by number of bits specified by immediate",
        BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0010011"
    ) { stmt ->
        // Uses >>> because 0 fill
        val newValue = (registerFile.getIntValue(stmt.getOperand(1))!! ushr stmt.getOperand(2)).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val SRLI64 = basicInstruction(
        "srli t1,t2,33",
        "Shift right logical : Set t1 to result of shifting t2 right by number of bits specified by immediate",
        BasicInstructionFormat.R_FORMAT, "000000 tttttt sssss 101 fffff 0010011"
    ) { stmt ->
        // Uses >>> because 0 fill
        val value: Long? = registerFile.getLongValue(stmt.getOperand(1))
        val shifted = value!! ushr stmt.getOperand(2)
        registerFile.updateRegisterByNumber(stmt.getOperand(0), shifted).ignoreOk()
    }

    @JvmField
    val SRLIW = basicInstruction(
        "srliw t1,t2,10",
        "Shift right logical (32 bit): Set t1 to result of shifting t2 right by number of bits specified by " +
                "immediate",
        BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0011011"
    ) { stmt ->
        val newValue = (registerFile.getIntValue(stmt.getOperand(1))!! ushr stmt.getOperand(2)).toLong()
        registerFile.updateRegisterByNumber(stmt.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val URET = basicInstruction(
        "uret", "Return from handling an interrupt or exception (to uepc)",
        BasicInstructionFormat.I_FORMAT, "000000000010 00000 000 00000 1110011"
    ) { stmt ->
        either {
            val upie = (csrRegisterFile.getIntValue("ustatus")!! and 0x10) == 0x10
            // Clear UPIE
            csrRegisterFile.updateRegisterByName(
                "ustatus",
                csrRegisterFile.getLongValue("ustatus")!! and 0x10L.inv()
            ).bind()
            val newUstatusValue = csrRegisterFile.getLongValue("ustatus")!!.let {
                if (upie) it or 0x1L else it and 0x1L.inv()
            }
            csrRegisterFile.updateRegisterByName("ustatus", newUstatusValue).bind()
            registerFile.setProgramCounter(csrRegisterFile.getIntValue("uepc")!!)
        }
    }

    @JvmField
    val WFI = basicInstruction(
        "wfi", "Wait for Interrupt",
        BasicInstructionFormat.I_FORMAT, "000100000101 00000 000 00000 1110011"
    ) { WaitEvent.left() }
}