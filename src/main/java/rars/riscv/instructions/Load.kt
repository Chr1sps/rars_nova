package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.MemoryError
import rars.exceptions.SimulationError
import rars.exceptions.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.Memory
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
 * Base class for all Load instructions
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Load private constructor(
    usage: String,
    description: String,
    funct: String,
    private val load: (Int, Memory) -> Either<MemoryError, Long>
) : BasicInstruction(
    usage,
    description,
    BasicInstructionFormat.I_FORMAT,
    "ssssssssssss ttttt $funct fffff 0000011"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val upperImmediate: Int = (statement.getOperand(1) shl 20) shr 20
        val newValue = load(
            registerFile.getIntValue(statement.getOperand(2))!! + upperImmediate,
            memory
        ).mapLeft { error ->
            SimulationError.create(statement, error)
        }.bind()
        registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).bind()
    }

    companion object {
        private fun load(
            usage: String,
            description: String,
            funct: String,
            load: (Int, Memory) -> Either<MemoryError, Long>
        ): Load = Load(usage, description, funct, load)

        @JvmField
        val LB = load(
            "lb t1, -100(t2)",
            "Set t1 to sign-extended 8-bit value from effective memory byte address",
            "000"
        ) { address, memory -> memory.getByte(address).map { it.toLong() } }

        @JvmField
        val LBU = load(
            "lbu t1, -100(t2)",
            "Set t1 to zero-extended 8-bit value from effective memory byte address",
            "100"
        ) { address, memory -> memory.getByte(address).map { it.toLong() and 0xFFL } }

        @JvmField
        val LH = load(
            "lh t1, -100(t2)",
            "Set t1 to sign-extended 16-bit value from effective memory halfword address",
            "001"
        ) { address, memory -> memory.getHalf(address).map { it.toLong() } }

        @JvmField
        val LHU = load(
            "lhu t1, -100(t2)",
            "Set t1 to zero-extended 16-bit value from effective memory halfword address",
            "101"
        ) { address, memory -> memory.getHalf(address).map { it.toLong() and 0xFFFFL } }

        @JvmField
        val LW = load(
            "lw t1, -100(t2)",
            "Set t1 to contents of effective memory word address",
            "010"
        ) { address, memory -> memory.getWord(address).map { it.toLong() } }

        @JvmField
        val LD = load(
            "ld t1, -100(t2)",
            "Set t1 to contents of effective memory double word address",
            "011"
        ) { address, memory -> memory.getDoubleWord(address) }

        @JvmField
        val LWU = load(
            "lwu t1, -100(t2)", "Set t1 to contents of effective memory word address without sign-extension", "110"
        ) { address, memory -> memory.getWord(address).map { it.toLong() and 0xFFFFFFFFL } }
    }
}
