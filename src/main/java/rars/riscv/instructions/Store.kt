package rars.riscv.instructions

import arrow.core.Either
import rars.ProgramStatement
import rars.exceptions.MemoryError
import rars.exceptions.SimulationError
import rars.exceptions.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.Memory
import rars.simulator.SimulationContext
import rars.util.ignoreOk

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
 * Base class for all Store instructions
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Store(
    usage: String,
    description: String,
    funct: String,
    private val store: (Int, Long, Memory) -> Either<MemoryError, Unit>
) : BasicInstruction(
    usage,
    description,
    BasicInstructionFormat.S_FORMAT,
    "sssssss fffff ttttt $funct sssss 0100011"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> {
        val upperImmediate: Int = (statement.getOperand(1) shl 20) shr 20
        return store(
            registerFile.getIntValue(statement.getOperand(2))!! + upperImmediate,
            registerFile.getLongValue(statement.getOperand(0))!!,
            memory
        ).mapLeft { error ->
            SimulationError.create(statement, error)
        }
    }

    companion object {
        private fun store(
            usage: String,
            description: String,
            funct: String,
            store: (Int, Long, Memory) -> Either<MemoryError, Unit>
        ) = Store(usage, description, funct, store)

        @JvmField
        val SB = store(
            "sb t1, -100(t2)",
            "Store byte : Store the low-order 8 bits of t1 into the effective memory byte address",
            "000"
        ) { address, value, memory -> memory.setByte(address, value.and(0xFFL).toByte()).ignoreOk() }

        @JvmField
        val SH = store(
            "sh t1, -100(t2)",
            "Store halfword : Store the low-order 16 bits of t1 into the effective memory halfword address",
            "001"
        ) { address, value, memory -> memory.setHalf(address, value.and(0xFFFFL).toShort()).ignoreOk() }

        @JvmField
        val SW = store(
            "sw t1, -100(t2)",
            "Store word : Store contents of t1 into effective memory word address",
            "010"
        ) { address, value, memory -> memory.setWord(address, value.toInt()).ignoreOk() }

        @JvmField
        val SD = store(
            "sd t1, -100(t2)",
            "Store double word : Store contents of t1 into effective memory double word address",
            "011"
        ) { address, value, memory -> memory.setDoubleWord(address, value).ignoreOk() }
    }
}