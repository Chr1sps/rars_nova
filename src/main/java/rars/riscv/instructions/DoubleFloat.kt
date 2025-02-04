package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.jsoftfloat.Environment
import rars.jsoftfloat.operations.Arithmetic
import rars.jsoftfloat.operations.Comparisons
import rars.jsoftfloat.types.Float64
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.instructions.Floating.FloatingUtils
import rars.simulator.SimulationContext

class DoubleFloat private constructor(
    usage: String,
    description: String,
    funct: String,
    rm: String,
    private val compute: (Float64, Float64, Environment) -> Float64
) : BasicInstruction(usage, description, BasicInstructionFormat.R_FORMAT, "$funct ttttt sssss $rm fffff 1010011") {

    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val environment = Environment()
        if (statement.hasOperand(3)) {
            environment.mode = FloatingUtils.getRoundingMode(
                statement.getOperand(3),
                statement,
                csrRegisterFile
            ).bind()
        }
        val result: Float64 = compute(
            Float64(fpRegisterFile.getLongValue(statement.getOperand(1))!!),
            Float64(fpRegisterFile.getLongValue(statement.getOperand(2))!!),
            environment
        )
        FloatingUtils.setfflags(csrRegisterFile, environment).bind()
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result.bits).bind()
    }

    companion object {
        private fun double(
            name: String,
            description: String,
            funct: String,
            compute: (Float64, Float64, Environment) -> Float64
        ): DoubleFloat = DoubleFloat("$name f1, f2, f3, dyn", description, funct, "qqq", compute)

        private fun double(
            name: String,
            description: String,
            funct: String,
            rm: String,
            compute: (Float64, Float64, Environment) -> Float64
        ): DoubleFloat = DoubleFloat("$name f1, f2, f3", description, funct, rm, compute)

        @JvmField
        val FADDD = double(
            "fadd.d", "Floating ADD (64 bit): assigns f1 to f2 + f3", "0000001"
        ) { f1, f2, e -> Arithmetic.add(f1, f2, e) }

        @JvmField
        val FDIVD = double(
            "fdiv.d", "Floating DIVide (64 bit): assigns f1 to f2 / f3", "0001101"
        ) { f1, f2, e -> Arithmetic.division(f1, f2, e) }

        @JvmField
        val FMAXD = double(
            "fmax.d", "Floating MAXimum (64 bit): assigns f1 to the larger of f1 and f3", "0010101", "001"
        ) { f1, f2, e -> Comparisons.maximumNumber(f1, f2, e) }

        @JvmField
        val FMIND = double(
            "fmin.d", "Floating MINimum (64 bit): assigns f1 to the smaller of f1 and f3", "0010101", "000"
        ) { f1, f2, e -> Comparisons.minimumNumber(f1, f2, e) }

        @JvmField
        val FMULD = double(
            "fmul.d", "Floating MULtiply (64 bit): assigns f1 to f2 * f3", "0001001"
        ) { f1, f2, e -> Arithmetic.multiplication(f1, f2, e) }

        @JvmField
        val FSUBD = double(
            "fsub.d", "Floating SUBtract (64 bit): assigns f1 to f2 - f3", "0000101"
        ) { f1, f2, e -> Arithmetic.subtraction(f1, f2, e) }
    }
}
