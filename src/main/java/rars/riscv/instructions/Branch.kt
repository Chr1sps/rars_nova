package rars.riscv.instructions

import arrow.core.Either
import arrow.core.right
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.hardware.registerfiles.RegisterFile
import rars.simulator.SimulationContext

/**
 * Base class for all branching instructions
 *
 * Created mainly for simplifying the branch simulator code, but also does
 * help with code reuse
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class Branch private constructor(
    operand: String,
    description: String,
    funct: String,
    val willBranch: (ProgramStatement, RegisterFile) -> Boolean
) : BasicInstruction(
    "$operand t1,t2,label",
    description,
    BasicInstructionFormat.B_FORMAT,
    "ttttttt sssss fffff $funct ttttt 1100011 "
) {
    override suspend fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> {
        if (willBranch(statement, registerFile)) {
            val instructionLength = this@Branch.instructionLength
            // Decrement needed because PC has already been incremented
            registerFile.programCounter =
                (registerFile.programCounter + statement.getOperand(2) - instructionLength)
        }
        return Unit.right()
    }

    companion object {
        val BEQ: Branch = makeBranch(
            "beq",
            "Branch if equal : Branch to statement at label's address if t1 and t2 are equal",
            "000"
        ) { first, other ->
            getLong(first)!! == getLong(other)!!
        }
        val BGE: Branch = makeBranch(
            "bge",
            "Branch if greater than or equal: Branch to statement at label's address if t1 is greater than or equal " +
                "to t2",
            "101"
        ) { first, other ->
            getLong(first)!! >= getLong(other)!!
        }
        val BGEU: Branch = makeBranch(
            "bgeu",
            "Branch if greater than or equal to (unsigned): Branch to statement at label's address if t1 is greater " +
                "than or equal to t2 (with an unsigned interpretation)",
            "111"
        ) { first, other ->
            getLong(first)!!.toULong() >= getLong(other)!!.toULong()
        }
        val BLT: Branch = makeBranch(
            "blt",
            "Branch if less than: Branch to statement at label's address if t1 is less than t2",
            "100"
        ) { first, other ->
            getLong(first)!! < getLong(other)!!
        }
        val BLTU: Branch = makeBranch(
            "bltu",
            "Branch if less than (unsigned): Branch to statement at label's address if t1 is less than t2 (with an " +
                "unsigned interpretation)",
            "110"
        ) { first, other ->
            getLong(first)!!.toULong() < getLong(other)!!.toULong()
        }
        val BNE: Branch = makeBranch(
            "bne",
            "Branch if not equal : Branch to statement at label's address if t1 and t2 are not equal",
            "001"
        ) { first, other ->
            getLong(first)!! != getLong(other)!!
        }

        private fun makeBranch(
            usage: String,
            description: String,
            funct: String,
            willBranch: RegisterFile.(Int, Int) -> Boolean
        ) = Branch(
            usage,
            description,
            funct
        ) { statement, registerFile ->
            statement.run {
                registerFile.willBranch(getOperand(0), getOperand(1))
            }
        }
    }
}
