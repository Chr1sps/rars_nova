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
import rars.jsoftfloat.operations.Comparisons
import rars.jsoftfloat.types.Float32
import rars.jsoftfloat.types.Float64
import rars.jsoftfloat.types.Floating
import rars.riscv.*
import rars.riscv.BasicInstruction.Companion.BASIC_INSTRUCTION_LENGTH
import rars.riscv.hardware.registerFiles.RegisterFile
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
    val CSRRCI = basicInstruction(
        "csrrci t0, fcsr, 10",
        "Atomic Read/Clear CSR Immediate: read from the CSR into t0 and clear bits of the CSR according to a " +
                "constant",
        BasicInstructionFormat.I_FORMAT,
        "ssssssssssss ttttt 111 fffff 1110011"
    ) { statement ->
        either {
            val csr = ensureNotNull(csrRegisterFile.getLongValue(statement.getOperand(1))) {
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
                    previousValue!! and statement.getOperand(2).toLong().inv() // TODO: Validate the long conversion
                ).bind()
            }
            registerFile.updateRegisterByNumber(statement.getOperand(0), csr).bind()
        }
    }

    @JvmField
    val CSRRS = basicInstruction(
        "csrrs t0, fcsr, t1", "Atomic Read/Set CSR: read from the CSR into t0 and logical or t1 into the CSR",
        BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 010 fffff 1110011"
    ) { statement ->
        either {
            val csr = ensureNotNull(csrRegisterFile.getLongValue(statement.getOperand(1))) {
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
                    previousValue!! or registerFile.getLongValue(statement.getOperand(2))!!
                ).bind()
            }
            registerFile.updateRegisterByNumber(statement.getOperand(0), csr).bind()
        }
    }

    @JvmField
    val CSRRSI = basicInstruction(
        "csrrsi t0, fcsr, 10",
        "Atomic Read/Set CSR Immediate: read from the CSR into t0 and logical or a constant into the CSR",
        BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 110 fffff 1110011"
    ) { statement ->
        either {
            val csr = ensureNotNull(csrRegisterFile.getLongValue(statement.getOperand(1))) {
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
                    previousValue!! or statement.getOperand(2).toLong()
                ).bind()
            }
            registerFile.updateRegisterByNumber(statement.getOperand(0), csr).bind()
        }
    }

    @JvmField
    val CSRRW = basicInstruction(
        "csrrw t0, fcsr, t1", "Atomic Read/Write CSR: read from the CSR into t0 and write t1 into the CSR",
        BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 001 fffff 1110011"
    ) { statement ->
        either {
            val csr = ensureNotNull(
                csrRegisterFile.getLongValue(statement.getOperand(1))
            ) {
                SimulationError.create(
                    statement,
                    "Attempt to access unavailable CSR",
                    ExceptionReason.ILLEGAL_INSTRUCTION
                )
            }
            val newValue = registerFile.getLongValue(statement.getOperand(2))!!
            csrRegisterFile.updateRegisterByNumber(
                statement.getOperand(1),
                newValue
            ).bind()
            registerFile.updateRegisterByNumber(statement.getOperand(0), csr).bind()
        }
    }

    @JvmField
    val CSRRWI = basicInstruction(
        "csrrwi t0, fcsr, 10",
        "Atomic Read/Write CSR Immediate: read from the CSR into t0 and write a constant into the CSR",
        BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 101 fffff 1110011"
    ) { statement ->
        either {
            val csr = ensureNotNull(
                csrRegisterFile.getLongValue(statement.getOperand(1))
            ) {
                SimulationError.create(
                    statement,
                    "Attempt to access unavailable CSR",
                    ExceptionReason.ILLEGAL_INSTRUCTION
                )
            }
            val newValue = statement.getOperand(2).toLong()
            csrRegisterFile.updateRegisterByNumber(
                statement.getOperand(1),
                newValue
            ).bind()
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
    val FCLASSD = basicInstruction(
        "fclass.d t1, f1", "Classify a floating point number (64 bit)",
        BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 001 fffff 1010011"
    ) { statement ->
        val input = fpRegisterFile.getFloat64(statement.getOperand(1))
        registerFile.fclass(input, statement.getOperand(0))
    }

    @JvmField
    val FCLASSS = basicInstruction(
        "fclass.s t1, f1", "Classify a floating point number",
        BasicInstructionFormat.I_FORMAT, "1110000 00000 sssss 001 fffff 1010011"
    ) { statement ->
        val input = fpRegisterFile.getFloat32(statement.getOperand(1))
        registerFile.fclass(input, statement.getOperand(0))
    }

    /**
     * Sets the one bit in t1 for every number
     * 0 t1 is −infinity.
     * 1 t1 is a negative normal number.
     * 2 t1 is a negative subnormal number.
     * 3 t1 is −0.
     * 4 t1 is +0.
     * 5 t1 is a positive subnormal number.
     * 6 t1 is a positive normal number.
     * 7 t1 is +infinity.
     * 8 t1 is a signaling NaN (Not implemented due to Java).
     * 9 t1 is a quiet NaN.
     */
    private fun <T : Floating<T>> RegisterFile.fclass(
        input: T,
        registerNumber: Int
    ): Either<SimulationError, Unit> {
        val newValue = if (input.isNaN) {
            if (input.isSignalling) 0x100 else 0x200
        } else {
            val isNegative = input.isSignMinus
            when {
                input.isInfinite -> if (isNegative) 0x001 else 0x080
                input.isZero -> if (isNegative) 0x008 else 0x010
                input.isSubnormal -> if (isNegative) 0x004 else 0x020
                else -> if (isNegative) 0x002 else 0x040
            }
        }
        return updateRegisterByNumber(registerNumber, newValue.toLong()).ignoreOk()
    }

    @JvmField
    val FLTD = basicInstruction(
        "flt.d t1, f1, f2", "Floating Less Than (64 bit): if f1 < f2, set t1 to 1, else set t1 to 0",
        BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 001 fffff 1010011"
    ) { statement ->
        val f1 = fpRegisterFile.getFloat64(statement.getOperand(1))
        val f2 = fpRegisterFile.getFloat64(statement.getOperand(2))
        val environment = Environment()
        val result = Comparisons.compareSignalingLessThan(f1, f2, environment)
        either {
            csrRegisterFile.setfflags(environment).bind()
            val newValue = if (result) 1L else 0L
            registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
        }
    }

    @JvmField
    val FLTS = basicInstruction(
        "flt.s t1, f1, f2", "Floating Less Than: if f1 < f2, set t1 to 1, else set t1 to 0",
        BasicInstructionFormat.R_FORMAT, "1010000 ttttt sssss 001 fffff 1010011"
    ) { statement ->
        val f1 = fpRegisterFile.getFloat32(statement.getOperand(1))
        val f2 = fpRegisterFile.getFloat32(statement.getOperand(2))
        val environment = Environment()
        val result = Comparisons.compareSignalingLessThan(f1, f2, environment)
        either {
            csrRegisterFile.setfflags(environment).bind()
            val newValue = if (result) 1L else 0L
            registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
        }
    }

    @JvmField
    val FLW = basicInstruction(
        "flw f1, -100(t1)", "Load a float from memory",
        BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 010 fffff 0000111"
    ) { statement ->
        val upperImmediate = (statement.getOperand(1) shl 20) shr 20
        try {
            val valueAddress = registerFile.getIntValue(statement.getOperand(2))!! + upperImmediate
            fpRegisterFile.updateRegisterByNumberInt(
                statement.getOperand(0),
                memory.getWord(valueAddress)
            )
        } catch (e: AddressErrorException) {
            SimulationError.create(statement, e).left()
        }
    }

    @JvmField
    val FMVDX = basicInstruction(
        "fmv.d.x f1, t1", "Move float: move bits representing a double from an 64 bit integer register",
        BasicInstructionFormat.I_FORMAT, "1111001 00000 sssss 000 fffff 1010011"
    ) { statement ->
        val newValue = registerFile.getLongValue(statement.getOperand(1))!!
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val FMVSX = basicInstruction(
        "fmv.s.x f1, t1", "Move float: move bits representing a float from an integer register",
        BasicInstructionFormat.I_FORMAT, "1111000 00000 sssss 000 fffff 1010011"
    ) { statement ->
        val newValue = fpRegisterFile.getIntValue(statement.getOperand(1))!!.toLong()
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val FMVXD = basicInstruction(
        "fmv.x.d t1, f1", "Move double: move bits representing a double to an 64 bit integer register",
        BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 000 fffff 1010011"
    ) { statement ->
        val newValue = fpRegisterFile.getLongValue(statement.getOperand(1))!!.toLong()
        registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val FMVXS = basicInstruction(
        "fmv.x.s t1, f1", "Move float: move bits representing a float to an integer register",
        BasicInstructionFormat.I_FORMAT, "1110000 00000 sssss 000 fffff 1010011"
    ) { statement ->
        val newValue = fpRegisterFile.getLongValue(statement.getOperand(1))!!.toLong()
        registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    @JvmField
    val FSD = basicInstruction(
        "fsd f1, -100(t1)", "Store a double to memory",
        BasicInstructionFormat.S_FORMAT, "sssssss fffff ttttt 011 sssss 0100111"
    ) { statement ->
        val upperImmediate = (statement.getOperand(1) shl 20) shr 20
        val address = registerFile.getIntValue(statement.getOperand(2))!! + upperImmediate
        val value = fpRegisterFile.getLongValue(statement.getOperand(0))!!
        try {
            memory.setDoubleWord(address, value)
            Unit.right()
        } catch (e: AddressErrorException) {
            SimulationError.create(statement, e).left()
        }
    }

    @JvmField
    val FSGNJD = basicInstruction(
        "fsgnj.d f1, f2, f3",
        "Floating point sign injection (64 bit): replace the sign bit of f2 with the sign bit of f3 and assign it" +
                " to f1",
        BasicInstructionFormat.R_FORMAT,
        "0010001 ttttt sssss 000 fffff 1010011"
    ) { statement ->
        val result = ((fpRegisterFile.getLongValue(statement.getOperand(1))!! and Long.MAX_VALUE)
                or (fpRegisterFile.getLongValue(statement.getOperand(2))!! and Long.MIN_VALUE))
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result).ignoreOk()
    }

    @JvmField
    val FSGNJND = basicInstruction(
        "fsgnjn.d f1, f2, f3",
        "Floating point sign injection (inverted 64 bit):  replace the sign bit of f2 with the opposite of sign " +
                "bit of f3 and assign it to f1",
        BasicInstructionFormat.R_FORMAT,
        "0010001 ttttt sssss 001 fffff 1010011"
    ) { statement ->
        val result = ((fpRegisterFile.getLongValue(statement.getOperand(1))!! and Long.MAX_VALUE)
                or ((fpRegisterFile.getLongValue(statement.getOperand(2))!!).inv() and Long.MIN_VALUE))
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result).ignoreOk()
    }

    @JvmField
    val FSGNJNS = basicInstruction(
        "fsgnjn.s f1, f2, f3",
        "Floating point sign injection (inverted):  replace the sign bit of f2 with the opposite of sign bit of " +
                "f3 and assign it to f1",
        BasicInstructionFormat.R_FORMAT,
        "0010000 ttttt sssss 001 fffff 1010011"
    ) { statement ->
        val result = ((fpRegisterFile.getIntValue(statement.getOperand(1))!! and 0x7FFFFFFF)
                or ((fpRegisterFile.getIntValue(statement.getOperand(2))!!).inv() and -0x80000000))
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result)
    }

    @JvmField
    val FSGNJS = basicInstruction(
        "fsgnj.s f1, f2, f3",
        "Floating point sign injection: replace the sign bit of f2 with the sign bit of f3 and assign it to f1",
        BasicInstructionFormat.R_FORMAT, "0010000 ttttt sssss 000 fffff 1010011"
    ) { statement ->
        val result = ((fpRegisterFile.getIntValue(statement.getOperand(1))!! and 0x7FFFFFFF)
                or (fpRegisterFile.getIntValue(statement.getOperand(2))!! and -0x80000000))
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result)
    }

    @JvmField
    val FSGNJXD = basicInstruction(
        "fsgnjx.d f1, f2, f3",
        "Floating point sign injection (xor 64 bit):  xor the sign bit of f2 with the sign bit of f3 and assign " +
                "it to f1",
        BasicInstructionFormat.R_FORMAT,
        "0010001 ttttt sssss 010 fffff 1010011"
    ) { statement ->
        val f2 = fpRegisterFile.getLongValue(statement.getOperand(1))!!
        val f3 = fpRegisterFile.getLongValue(statement.getOperand(2))!!
        val result = (f2 and 0x7FFFFFFF_FFFFFFFFL) or ((f2 xor f3) and Long.MIN_VALUE)
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result).ignoreOk()
    }

    @JvmField
    val FSGNJXS = basicInstruction(
        "fsgnjx.s f1, f2, f3",
        "Floating point sign injection (xor):  xor the sign bit of f2 with the sign bit of f3 and assign it to f1",
        BasicInstructionFormat.R_FORMAT,
        "0010000 ttttt sssss 010 fffff 1010011"
    ) { statement ->
        val f2 = fpRegisterFile.getIntValue(statement.getOperand(1))!!
        val f3 = fpRegisterFile.getIntValue(statement.getOperand(2))!!
        val result = (f2 and 0x7FFFFFFF) or ((f2 xor f3) and -0x80000000)
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result)
    }

    @JvmField
    val FSQRTD = basicInstruction(
        "fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
        BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011"
    ) { stmt ->
        either {
            val environment = Environment()
            environment.mode = csrRegisterFile.getRoundingMode(
                stmt.getOperand(2),
                stmt
            ).bind()
            val registerValue = fpRegisterFile.getLongValue(stmt.getOperand(1))!!
            val result = Arithmetic.squareRoot(Float64(registerValue), environment)
            csrRegisterFile.setfflags(environment).bind()
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
            environment.mode = csrRegisterFile.getRoundingMode(
                stmt.getOperand(2),
                stmt
            ).bind()
            val registerValue = fpRegisterFile.getIntValue(stmt.getOperand(1))!!
            val result = Arithmetic.squareRoot(Float32(registerValue), environment)
            csrRegisterFile.setfflags(environment).bind()
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