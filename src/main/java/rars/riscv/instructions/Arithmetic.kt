package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.InstructionsRegistry
import rars.simulator.SimulationContext
import java.math.BigInteger

/*
Copyright (c) 2017,  Benjamin Landers

Developed by Benjamin Landers (benjaminrlanders@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */
/**
 * Base class for all integer instructions using immediates
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Arithmetic private constructor(
    usage: String,
    description: String,
    funct7: String,
    funct3: String,
    @JvmField val compute: (Long, Long) -> Long,
    @JvmField val computeW: (Int, Int) -> Int
) : BasicInstruction(
    usage, description, BasicInstructionFormat.R_FORMAT,
    "$funct7 ttttt sssss $funct3 fffff 0110011"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        if (InstructionsRegistry.RV64_MODE_FLAG) {
            val newValue = compute(
                registerFile.getLongValue(statement.getOperand(1))!!,
                registerFile.getLongValue(statement.getOperand(2))!!
            )
            registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
        } else {
            val newValue = computeW(
                registerFile.getIntValue(statement.getOperand(1))!!,
                registerFile.getIntValue(statement.getOperand(2))!!
            ).toLong()
            registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
        }
    }

    companion object {
        @JvmStatic
        private fun arithmetic(
            usage: String,
            description: String,
            funct7: String,
            funct3: String,
            compute: (Long, Long) -> Long
        ): Arithmetic = Arithmetic(usage, description, funct7, funct3, compute) { first, other ->
            compute(first.toLong(), other.toLong()).toInt()
        }

        @JvmStatic
        private fun arithmetic(
            usage: String,
            description: String,
            funct7: String,
            funct3: String,
            computeW: (Int, Int) -> Int,
            compute: (Long, Long) -> Long,
        ): Arithmetic = Arithmetic(usage, description, funct7, funct3, compute, computeW)

        @JvmField
        val ADD = arithmetic(
            "add t1,t2,t3", "Addition: set t1 to (t2 plus t3)",
            "0000000", "000", Long::plus
        )

        @JvmField
        val AND = arithmetic(
            "and t1,t2,t3", "Bitwise AND : Set t1 to bitwise AND of t2 and t3",
            "0000000", "111", Long::and
        )

        @JvmField
        val DIV = arithmetic(
            "div t1,t2,t3", "Division: set t1 to the result of t2/t3",
            "0000001", "100"
        ) { first, other ->
            if (other == 0L) -1
            else first / other
        }

        @JvmField
        val DIVU = arithmetic(
            "divu t1,t2,t3", "Division: set t1 to the result of t2/t3 using unsigned division",
            "0000001", "101", { first, other ->
                if (other == 0) -1
                else (first.toUInt() / other.toUInt()).toInt()
            }
        ) { first, other ->
            if (other == 0L) -1
            else (first.toULong() / other.toULong()).toLong()
        }

        @JvmField
        val MUL = arithmetic(
            "mul t1,t2,t3", "Multiplication: set t1 to the lower 32 bits of t2*t3",
            "0000001", "000", Long::times
        )

        @JvmField
        val MULH = arithmetic(
            "mulh t1,t2,t3", "Multiplication: set t1 to the upper 32 bits of t2*t3 using signed multiplication",
            "0000001", "001", { first, other ->
                (first.toULong() * other.toULong()).shr(32).toInt()
            }
        ) { first, other -> (first * other) shr 32 }
        // return BigInteger.valueOf(value).multiply(BigInteger.valueOf(value2)).shiftRight(64).toLong()

        @JvmField
        val MULHSU = arithmetic(
            "mulhsu t1,t2,t3",
            "Multiplication: set t1 to the upper 32 bits of t2*t3 where t2 is signed and t3 is unsigned",
            "0000001",
            "010",
            { first, other ->
                (first.toULong() * other.lowerToULong()).shr(32).toInt()
            }
        ) { first, other ->
            first.toBigInteger()
                .times(other.toULong().toBigInteger())
                .shr(64)
                .toLong()
        }

        @JvmField
        val MULHU = arithmetic(
            "mulhu t1,t2,t3", "Multiplication: set t1 to the upper 32 bits of t2*t3 using unsigned multiplication",
            "0000001", "011", { first, other ->
                first.lowerToULong()
                    .times(other.lowerToULong())
                    .shr(32).toInt()
            }
        ) { first, other ->
            first.toULong().toBigInteger()
                .times(other.toULong().toBigInteger())
                .shr(64).toLong()
        }

        @JvmField
        val OR = arithmetic(
            "or t1,t2,t3", "Bitwise OR : Set t1 to bitwise OR of t2 and t3",
            "0000000", "110", Long::or
        )

        @JvmField
        val REM = arithmetic(
            "rem t1,t2,t3", "Remainder: set t1 to the remainder of t2/t3",
            "0000001", "110"
        ) { first, other ->
            if (other == 0L) first
            else first.rem(other)
        }

        @JvmField
        val REMU = arithmetic(
            "remu t1,t2,t3", "Remainder: set t1 to the remainder of t2/t3 using unsigned division",
            "0000001", "111", { first, other ->
                if (other == 0) first
                else first.toUInt().rem(other.toUInt()).toInt()
            }
        ) { first, other ->
            if (other == 0L) first
            else first.toULong().rem(other.toULong()).toLong()
        }

        @JvmField
        val SLL = arithmetic(
            "sll t1,t2,t3",
            "Shift left logical: Set t1 to result of shifting t2 left by number of bits specified by value in " +
                    "low-order 5 bits of t3",
            "0000000",
            "001",
            { first, other -> first.shl(other.and(0b0001_1111)) } // use the bottom 5 bits
        ) { first, other ->
            first.shl(other.and(0b0011_1111L).toInt()) // use the bottom 6 bits
        }

        @JvmField
        val SLT = arithmetic(
            "slt t1,t2,t3", "Set less than : If t2 is less than t3, then set t1 to 1 else set t1 to 0",
            "0000000", "010"
        ) { first, other -> if (first < other) 1 else 0 }

        @JvmField
        val SLTU = arithmetic(
            "sltu t1,t2,t3",
            "Set less than : If t2 is less than t3 using unsigned comparision, then set t1 to 1 else set t1 to 0",
            "0000000",
            "011"
        ) { first, other -> if (first.toULong() < other.toULong()) 1 else 0 }

        @JvmField
        val SRA = arithmetic(
            "sra t1,t2,t3",
            "Shift right arithmetic: Set t1 to result of sign-extended shifting t2 right by number of bits specified " +
                    "by value in low-order 5 bits of t3",
            "0100000",
            "101",
            { first, other ->
                first.shr(other and 0b0001_1111) // use the bottom 5 bits
            }
        ) { first, other ->
            first.shr(other.and(0b0011_1111L).toInt()) // use the bottom 6 bits
        }

        @JvmField
        val SRL = arithmetic(
            "srl t1,t2,t3",
            "Shift right logical: Set t1 to result of shifting t2 right by number of bits specified by value in " +
                    "low-order 5 bits of t3",
            "0000000",
            "101", { first, other ->
                first.ushr(other and 0b0001_1111) // use the bottom 5 bits
            }
        ) { first, other ->
            first.ushr(other.and(0b0011_1111L).toInt()) // use the bottom 6 bits
        }

        @JvmField
        val SUB = arithmetic(
            "sub t1,t2,t3", "Subtraction: set t1 to (t2 minus t3)",
            "0100000", "000", Long::minus
        )

        @JvmField
        val XOR = arithmetic(
            "xor t1,t2,t3", "Bitwise XOR : Set t1 to bitwise XOR of t2 and t3",
            "0000000", "100", Long::xor
        )
    }
}


private fun Int.lowerToULong(): ULong = this.toULong() and 0xFFFFFFFFu
private fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)
private fun ULong.toBigInteger(): BigInteger {
    val converted = this.toLong()
    return BigInteger.valueOf(converted).let {
        if (converted < 0) it.add(BigInteger.ONE.shiftLeft(64))
        else it
    }
}