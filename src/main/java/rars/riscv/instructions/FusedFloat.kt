package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.jsoftfloat.Environment
import rars.jsoftfloat.operations.Arithmetic
import rars.jsoftfloat.types.Float32
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.getRoundingMode
import rars.riscv.setfflags
import rars.simulator.SimulationContext
import rars.util.flipRounding

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
class FusedFloat private constructor(
    usage: String,
    description: String,
    op: String,
    private val compute: (Float32, Float32, Float32, Environment) -> Float32
) : BasicInstruction(
    "$usage, dyn", description, BasicInstructionFormat.R4_FORMAT,
    "qqqqq 00 ttttt sssss ppp fffff 100${op}11"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val environment = Environment()
        environment.mode =
            csrRegisterFile.getRoundingMode(statement.getOperand(4), statement).bind()
        val result: Float32 = compute(
            Float32(fpRegisterFile.getIntValue(statement.getOperand(1))!!),
            Float32(fpRegisterFile.getIntValue(statement.getOperand(2))!!),
            Float32(fpRegisterFile.getIntValue(statement.getOperand(3))!!),
            environment
        )
        csrRegisterFile.setfflags(environment).bind()
        fpRegisterFile.updateRegisterByNumberInt(statement.getOperand(0), result.bits)
    }

    companion object {
        @JvmField
        val FMADDS = FusedFloat(
            "fmadd.s f1, f2, f3, f4", "Fused Multiply Add: Assigns f2*f3+f4 to f1", "00"
        ) { f1, f2, f3, env -> Arithmetic.fusedMultiplyAdd(f1, f2, f3, env) }

        @JvmField
        val FMSUBS = FusedFloat(
            "fmsub.s f1, f2, f3, f4", "Fused Multiply Subtract: Assigns f2*f3-f4 to f1", "01"
        ) { f1, f2, f3, env -> Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), env) }

        @JvmField
        val FNMADDS = FusedFloat(
            "fnmadd.s f1, f2, f3, f4", "Fused Negate Multiply Add: Assigns -(f2*f3+f4) to f1", "11"
        ) { f1, f2, f3, env ->
            env.flipRounding()
            Arithmetic.fusedMultiplyAdd(f1, f2, f3, env).negate()
        }

        @JvmField
        val FNMSUBS = FusedFloat(
            "fnmsub.s f1, f2, f3, f4", "Fused Negate Multiply Subtract: Assigns -(f2*f3-f4) to f1", "10"
        ) { f1, f2, f3, env ->
            env.flipRounding()
            Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), env).negate()
        }

//        @JvmField
//        val INSTRUCTIONS = arrayOf(FMADDS, FMSUBS, FNMADDS, FNMSUBS)
    }
}
