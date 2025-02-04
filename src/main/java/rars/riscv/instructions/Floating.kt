package rars.riscv.instructions

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import rars.ProgramStatement
import rars.exceptions.ExceptionReason
import rars.exceptions.SimulationError
import rars.exceptions.SimulationEvent
import rars.jsoftfloat.Environment
import rars.jsoftfloat.RoundingMode
import rars.jsoftfloat.operations.Arithmetic
import rars.jsoftfloat.operations.Comparisons
import rars.jsoftfloat.types.Float32
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile
import rars.simulator.SimulationContext

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
 * Base class for float to float operations
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Floating(
    usage: String,
    description: String,
    funct: String,
    rm: String,
    private val compute: (Float32, Float32, Environment) -> Float32
) : BasicInstruction(
    usage,
    description,
    BasicInstructionFormat.R_FORMAT,
    "$funct ttttt sssss $rm fffff 1010011"
) {

    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val environment = Environment()
        val hasRoundingMode: Boolean = statement.hasOperand(3)
        if (hasRoundingMode) {
            environment.mode = FloatingUtils.getRoundingMode(
                statement.getOperand(3),
                statement,
                csrRegisterFile
            ).bind()
        }
        val result: Float32 = compute(
            Float32(fpRegisterFile.getIntValue(statement.getOperand(1))!!),
            Float32(fpRegisterFile.getIntValue(statement.getOperand(2))!!),
            environment
        )
        FloatingUtils.setfflags(csrRegisterFile, environment).bind()
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result.bits).bind()
    }

    object FloatingUtils {
        @JvmStatic
        fun setfflags(csRegisterFile: CSRegisterFile, environment: Environment): Either<SimulationError, Unit> =
            either {
                val fflags = listOf(
                    environment.inexact to 1,
                    environment.underflow to 2,
                    environment.overflow to 4,
                    environment.divByZero to 8,
                    environment.invalid to 16
                ).filter { it.first }.sumOf { it.second }
                if (fflags != 0) {
                    csRegisterFile.updateRegisterByName(
                        "fflags",
                        csRegisterFile.getLongValue("fflags")!! or fflags.toLong()
                    ).bind()
                }
            }

        @JvmStatic
        fun getRoundingMode(
            rmValue: Int,
            statement: ProgramStatement,
            csRegisterFile: CSRegisterFile
        ): Either<SimulationError, RoundingMode> {
            val frm = csRegisterFile.getIntValue("frm")!!
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
                    ExceptionReason.OTHER
                ).left()
            }
        }

        @JvmStatic
        fun FloatingPointRegisterFile.getFloat32(num: Int): Float32 {
            return Float32(getIntValue(num)!!)
        }
    }

    companion object {
        private fun floating(
            name: String,
            description: String,
            funct: String,
            compute: (Float32, Float32, Environment) -> Float32,
        ): Floating = Floating(
            "$name f1, f2, f3, dyn",
            description,
            funct,
            "qqq",
            compute
        )

        private fun floating(
            name: String,
            description: String,
            funct: String,
            rm: String,
            compute: (Float32, Float32, Environment) -> Float32,
        ): Floating = Floating(
            "$name f1, f2, f3",
            description,
            funct,
            rm,
            compute
        )

        @JvmField
        val FADDS = floating(
            "fadd.s", "Floating ADD: assigns f1 to f2 + f3", "0000000"
        ) { f1, f2, env -> Arithmetic.add(f1, f2, env) }

        @JvmField
        val FDIVS = floating(
            "fdiv.s", "Floating DIVide: assigns f1 to f2 / f3", "0001100"
        ) { f1, f2, env -> Arithmetic.division(f1, f2, env) }

        @JvmField
        val FMAXS = floating(
            "fmax.s", "Floating MAXimum: assigns f1 to the larger of f1 and f3", "0010100", "001"
        ) { f1, f2, env -> Comparisons.maximumNumber(f1, f2, env) }

        @JvmField
        val FMINS = floating(
            "fmin.s", "Floating MINimum: assigns f1 to the smaller of f1 and f3", "0010100", "000"
        ) { f1, f2, env -> Comparisons.minimumNumber(f1, f2, env) }

        @JvmField
        val FMULS = floating(
            "fmul.s", "Floating MULtiply: assigns f1 to f2 * f3", "0001000"
        ) { f1, f2, env -> Arithmetic.multiplication(f1, f2, env) }

        @JvmField
        val FSUBS = floating(
            "fsub.s", "Floating SUBtract: assigns f1 to f2 - f3", "0000100"
        ) { f1, f2, env -> Arithmetic.subtraction(f1, f2, env) }
    }
}
