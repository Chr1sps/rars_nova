package rars.riscv.instructions

import arrow.core.Either
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.InstructionsRegistry
import rars.simulator.SimulationContext
import rars.util.ConversionUtils
import rars.util.ignoreOk

/**
 * Base class for all integer instructions using immediates
 *
 * @author Benjamin Landers
 * @version June 2017
 */
class ImmediateInstruction private constructor(
    usage: String,
    description: String,
    funct: String,
    isRV64: Boolean = false,
    @JvmField val compute: (Long, Long) -> Long
) : BasicInstruction(
    usage, description, BasicInstructionFormat.I_FORMAT,
    "tttttttttttt sssss $funct fffff 001${if (isRV64) "1" else "0"}011"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> {
        val upperImmediate = (statement.getOperand(2) shl 20) shr 20
        val newValue = if (InstructionsRegistry.RV64_MODE_FLAG) compute(
            registerFile.getLongValue(statement.getOperand(1))!!,
            upperImmediate.toLong()
        ) else computeW(
            registerFile.getIntValue(statement.getOperand(1))!!,
            upperImmediate
        ).toLong()
        return registerFile.updateRegisterByNumber(statement.getOperand(0), newValue).ignoreOk()
    }

    private fun computeW(value: Int, immediate: Int): Int =
        ConversionUtils.longLowerHalfToInt(compute(value.toLong(), immediate.toLong()))

    companion object {
        private fun immediate32(
            usage: String,
            description: String,
            funct: String,
            compute: (Long, Long) -> Long
        ): ImmediateInstruction = ImmediateInstruction(usage, description, funct, false, compute)

        private fun immediate64(
            usage: String,
            description: String,
            funct: String,
            compute: (Long, Long) -> Long
        ): ImmediateInstruction = ImmediateInstruction(usage, description, funct, true, compute)

        @JvmField
        val ADDI = immediate32(
            "addi t1,t2,-100", "Addition immediate: set t1 to (t2 plus signed 12-bit immediate)",
            "000", Long::plus
        )

        @JvmField
        val ADDIW = immediate64(
            "addiw t1,t2,-100",
            "Addition immediate: set t1 to (t2 plus signed 12-bit immediate) using only the lower 32 bits",
            "000",
        ) { first, other -> first.toInt().plus(other.toInt()).toLong() }

        @JvmField
        val ANDI = immediate32(
            "andi t1,t2,-100", "Bitwise AND immediate : Set t1 to bitwise AND of t2 and sign-extended 12-bit immediate",
            "111", Long::and
        )

        @JvmField
        val ORI = immediate32(
            "ori t1,t2,-100", "Bitwise OR immediate : Set t1 to bitwise OR of t2 and sign-extended 12-bit immediate",
            "110", Long::or
        )

        @JvmField
        val SLTI = immediate32(
            "slti t1,t2,-100",
            "Set less than immediate : If t2 is less than sign-extended 12-bit immediate, then set t1 to 1 else set t1 to 0",
            "010"
        ) { first, other -> if (first < other) 1 else 0 }

        @JvmField
        val SLTIU = immediate32(
            "sltiu t1,t2,-100",
            "Set less than immediate unsigned : If t2 is less than  sign-extended 16-bit immediate using unsigned comparison, then set t1 to 1 else set t1 to 0",
            "011"
        ) { first, other -> if (first.toULong() < other.toULong()) 1 else 0 }

        @JvmField
        val XORI = immediate32(
            "xori t1,t2,-100", "Bitwise XOR immediate : Set t1 to bitwise XOR of t2 and sign-extended 12-bit immediate",
            "100", Long::xor
        )
    }
}
