package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.ksoftfloat.Environment
import rars.ksoftfloat.operations.*
import rars.ksoftfloat.types.Float32
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.getRoundingMode
import rars.riscv.setfflags
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
    private val compute: Environment.(Float32, Float32) -> Float32
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
            environment.mode = csrRegisterFile.getRoundingMode(
                statement.getOperand(3),
                statement
            ).bind()
        }
        val result: Float32 = environment.compute(
            Float32(fpRegisterFile.getIntValue(statement.getOperand(1))!!),
            Float32(fpRegisterFile.getIntValue(statement.getOperand(2))!!),
        )
        csrRegisterFile.setfflags(environment).bind()
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result.bits).bind()
    }

    companion object {
        private fun floating(
            name: String,
            description: String,
            funct: String,
            compute: Environment.(Float32, Float32) -> Float32,
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
            compute: Environment.(Float32, Float32) -> Float32,
        ): Floating = Floating(
            "$name f1, f2, f3",
            description,
            funct,
            rm,
            compute
        )

        @JvmField
        val FADDS = floating(
            "fadd.s", "Floating ADD: assigns f1 to f2 + f3", "0000000",
            Float32::add
        )

        @JvmField
        val FDIVS = floating(
            "fdiv.s", "Floating DIVide: assigns f1 to f2 / f3", "0001100",
            Float32::divide
        )

        @JvmField
        val FMAXS = floating(
            "fmax.s", "Floating MAXimum: assigns f1 to the larger of f1 and f3",
            "0010100", "001",
            Float32::max
        )

        @JvmField
        val FMINS = floating(
            "fmin.s", "Floating MINimum: assigns f1 to the smaller of f1 and f3", "0010100", "000",
            Float32::min
        )

        @JvmField
        val FMULS = floating(
            "fmul.s", "Floating MULtiply: assigns f1 to f2 * f3", "0001000",
            Float32::multiply
        )

        @JvmField
        val FSUBS = floating(
            "fsub.s", "Floating SUBtract: assigns f1 to f2 - f3", "0000100",
            Float32::subtract
        )
    }
}
