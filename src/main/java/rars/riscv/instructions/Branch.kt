package rars.riscv.instructions

import arrow.core.Either
import arrow.core.right
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.registerfiles.RegisterFile
import rars.simulator.SimulationContext
import java.lang.Long
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Unit

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
 * Base class for all branching instructions
 *
 *
 * Created mainly for making the branch simulator code simpler, but also does
 * help with code reuse
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Branch private constructor(
    operand: String,
    description: String,
    funct: String,
    @JvmField val willBranch: (ProgramStatement, RegisterFile) -> Boolean
) : BasicInstruction(
    "$operand t1,t2,label",
    description,
    BasicInstructionFormat.B_FORMAT,
    "ttttttt sssss fffff $funct ttttt 1100011 "
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> {
        if (willBranch(statement, registerFile)) {
            val instructionLength = this@Branch.instructionLength
            // Decrement needed because PC has already been incremented
            registerFile.setProgramCounter(registerFile.programCounter + statement.getOperand(2) - instructionLength)
        }
        return Unit.right()
    }

    companion object {
        val BEQ: Branch = makeBranch(
            "beq",
            "Branch if equal : Branch to statement at label's address if t1 and t2 are equal",
            "000"
        ) { statement, registerFile ->
            registerFile.getLongValue(statement.getOperand(0)) ==
                registerFile.getLongValue(statement.getOperand(1))
        }
        val BGE: Branch = makeBranch(
            "bge",
            "Branch if greater than or equal: Branch to statement at label's address if t1 is greater than or equal " +
                "to t2",
            "101"
        ) { statement, registerFile ->
            registerFile.getLongValue(statement.getOperand(0))!! >=
                registerFile.getLongValue(statement.getOperand(1))!!
        }
        val BGEU: Branch = makeBranch(
            "bgeu",
            "Branch if greater than or equal to (unsigned): Branch to statement at label's address if t1 is greater " +
                "than or equal to t2 (with an unsigned interpretation)",
            "111"
        ) { statement, registerFile ->
            Long.compareUnsigned(
                registerFile.getLongValue(statement.getOperand(0))!!,
                registerFile.getLongValue(statement.getOperand(1))!!
            ) >= 0
        }
        val BLT: Branch = makeBranch(
            "blt",
            "Branch if less than: Branch to statement at label's address if t1 is less than t2",
            "100"
        ) { statement, registerFile ->
            registerFile.getLongValue(statement.getOperand(0))!! < registerFile
                .getLongValue(
                    statement.getOperand(1)
                )!!
        }
        val BLTU: Branch = makeBranch(
            "bltu",
            "Branch if less than (unsigned): Branch to statement at label's address if t1 is less than t2 (with an " +
                "unsigned interpretation)",
            "110"
        ) { statement, registerFile ->
            Long.compareUnsigned(
                registerFile.getLongValue(statement.getOperand(0))!!,
                registerFile.getLongValue(statement.getOperand(1))!!
            ) < 0
        }
        val BNE: Branch = makeBranch(
            "bne",
            "Branch if not equal : Branch to statement at label's address if t1 and t2 are not equal",
            "001"
        ) { statement, registerFile ->
            val firstValue = registerFile.getIntValue(statement.getOperand(0)) as Int
            val secondValue = registerFile.getIntValue(statement.getOperand(1)) as Int
            firstValue != secondValue
        }

        private fun makeBranch(
            usage: String,
            description: String,
            funct: String,
            willBranchCallback: (ProgramStatement, RegisterFile) -> Boolean
        ) = Branch(usage, description, funct, willBranchCallback)
    }
}
