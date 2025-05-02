package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.ksoftfloat.Environment
import rars.ksoftfloat.operations.*
import rars.ksoftfloat.types.Float64
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.setfflags
import rars.simulator.SimulationContext

class DoubleFloat private constructor(
    usage: String,
    description: String,
    funct: String,
    rm: String,
    private val compute: Environment.(Float64, Float64) -> Float64
) : BasicInstruction(
    usage,
    description,
    BasicInstructionFormat.R_FORMAT,
    "$funct ttttt sssss $rm fffff 1010011"
) {

    override suspend fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> =
        either {
            val environment = Environment()
            if (statement.hasOperand(3)) {
                environment.mode = csrRegisterFile.getRoundingMode(
                    statement.getOperand(3),
                    statement
                ).bind()
            }
            val result: Float64 = environment.compute(
                Float64(fpRegisterFile.getLong(statement.getOperand(1))!!),
                Float64(fpRegisterFile.getLong(statement.getOperand(2))!!),
            )
            csrRegisterFile.setfflags(environment).bind()
            fpRegisterFile.updateRegisterByNumber(
                statement.getOperand(0),
                result.bits
            ).bind()
        }

    companion object {
        private fun double(
            name: String,
            description: String,
            funct: String,
            compute: Environment.(Float64, Float64) -> Float64
        ): DoubleFloat = DoubleFloat(
            "$name f1, f2, f3, dyn",
            description,
            funct,
            "qqq",
            compute
        )

        private fun double(
            name: String,
            description: String,
            funct: String,
            rm: String,
            compute: Environment.(Float64, Float64) -> Float64
        ): DoubleFloat =
            DoubleFloat("$name f1, f2, f3", description, funct, rm, compute)

        val FADDD = double(
            "fadd.d", "Floating ADD (64 bit): assigns f1 to f2 + f3", "0000001",
            Float64::add
        )

        val FDIVD = double(
            "fdiv.d",
            "Floating DIVide (64 bit): assigns f1 to f2 / f3",
            "0001101",
            Float64::divide
        )

        val FMAXD = double(
            "fmax.d",
            "Floating MAXimum (64 bit): assigns f1 to the larger of f1 and f3",
            "0010101",
            "001",
            Float64::max
        )

        val FMIND = double(
            "fmin.d",
            "Floating MINimum (64 bit): assigns f1 to the smaller of f1 and f3",
            "0010101",
            "000",
            Float64::min
        )

        val FMULD = double(
            "fmul.d",
            "Floating MULtiply (64 bit): assigns f1 to f2 * f3",
            "0001001",
            Float64::multiply
        )

        val FSUBD = double(
            "fsub.d",
            "Floating SUBtract (64 bit): assigns f1 to f2 - f3",
            "0000101",
            Float64::subtract
        )
    }
}
