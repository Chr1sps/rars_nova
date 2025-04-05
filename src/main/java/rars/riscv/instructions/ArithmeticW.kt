package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.simulator.SimulationContext
import rars.util.ConversionUtils

class ArithmeticW private constructor(
    usage: String,
    description: String,
    funct7: String,
    funct3: String,
    private val base: Arithmetic
) : BasicInstruction(
    usage, description, BasicInstructionFormat.R_FORMAT,
    "$funct7 ttttt sssss $funct3 fffff 0111011"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val newValue = base.computeW(
            ConversionUtils.longLowerHalfToInt(registerFile.getLongValue(statement.getOperand(1))!!),
            ConversionUtils.longLowerHalfToInt(registerFile.getLongValue(statement.getOperand(2))!!)
        ).toLong()
        registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
    }

    companion object {
        @JvmField
        val ADDW = ArithmeticW(
            "addw t1,t2,t3", "Addition: set t1 to (t2 plus t3) using only the lower 32 bits",
            "0000000", "000", Arithmetic.ADD
        )

        @JvmField
        val DIVUW = ArithmeticW(
            "divuw t1,t2,t3", "Division: set t1 to the result of t2/t3 using unsigned division limited to 32 bits",
            "0000001", "101", Arithmetic.DIVU
        )

        @JvmField
        val DIVW = ArithmeticW(
            "divw t1,t2,t3", "Division: set t1 to the result of t2/t3 using only the lower 32 bits",
            "0000001", "100", Arithmetic.DIV
        )

        @JvmField
        val MULW = ArithmeticW(
            "mulw t1,t2,t3",
            "Multiplication: set t1 to the lower 32 bits of t2*t3 using only the lower 32 bits of the input",
            "0000001", "000", Arithmetic.MUL
        )

        @JvmField
        val REMUW = ArithmeticW(
            "remuw t1,t2,t3",
            "Remainder: set t1 to the remainder of t2/t3 using unsigned division limited to 32 bits",
            "0000001", "111", Arithmetic.REMU
        )

        @JvmField
        val REMW = ArithmeticW(
            "remw t1,t2,t3", "Remainder: set t1 to the remainder of t2/t3 using only the lower 32 bits",
            "0000001", "110", Arithmetic.REM
        )

        @JvmField
        val SLLW = ArithmeticW(
            "sllw t1,t2,t3",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by value in low-order 5 bits of t3",
            "0000000",
            "001",
            Arithmetic.SLL
        )

        @JvmField
        val SRAW = ArithmeticW(
            "sraw t1,t2,t3",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by value in low-order 5 bits of t3",
            "0100000",
            "101",
            Arithmetic.SRA
        )

        @JvmField
        val SRLW = ArithmeticW(
            "srlw t1,t2,t3",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by value in low-order 5 bits of t3",
            "0000000",
            "101",
            Arithmetic.SRL
        )

        @JvmField
        val SUBW = ArithmeticW(
            "subw t1,t2,t3", "Subtraction: set t1 to (t2 minus t3) using only the lower 32 bits",
            "0100000", "000", Arithmetic.SUB
        )
    }
}
