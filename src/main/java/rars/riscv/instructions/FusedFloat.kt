package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.ksoftfloat.Environment
import rars.ksoftfloat.operations.fusedMultiplyAdd
import rars.ksoftfloat.types.Float32
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.setfflags
import rars.simulator.SimulationContext
import rars.util.flipRounding

class FusedFloat private constructor(
    usage: String,
    description: String,
    op: String,
    private val compute: (Environment, Float32, Float32, Float32) -> Float32
) : BasicInstruction(
    "$usage, dyn", description, BasicInstructionFormat.R4_FORMAT,
    "qqqqq 00 ttttt sssss ppp fffff 100${op}11"
) {
    override suspend fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> =
        either {
            val environment = Environment().apply {
                mode = csrRegisterFile.getRoundingMode(
                    statement.getOperand(4),
                    statement
                ).bind()
            }
            val result = compute(
                environment,
                Float32(fpRegisterFile.getInt(statement.getOperand(1))!!),
                Float32(fpRegisterFile.getInt(statement.getOperand(2))!!),
                Float32(fpRegisterFile.getInt(statement.getOperand(3))!!),
            )
            csrRegisterFile.setfflags(environment).bind()
            fpRegisterFile.updateRegisterByNumberInt(
                statement.getOperand(0),
                result.bits
            )
        }

    companion object {
        val FMADDS = FusedFloat(
            "fmadd.s f1, f2, f3, f4",
            "Fused Multiply Add: Assigns f2*f3+f4 to f1",
            "00",
            Float32::fusedMultiplyAdd
        )

        val FMSUBS = FusedFloat(
            "fmsub.s f1, f2, f3, f4",
            "Fused Multiply Subtract: Assigns f2*f3-f4 to f1",
            "01",
        ) { env, f1, f2, f3 ->
            Float32.fusedMultiplyAdd(
                env,
                f1,
                f2,
                f3.negate()
            )
        }

        val FNMADDS = FusedFloat(
            "fnmadd.s f1, f2, f3, f4",
            "Fused Negate Multiply Add: Assigns -(f2*f3+f4) to f1",
            "11",
        ) { env, f1, f2, f3 ->
            // TODO: test if this is the right behaviour
            env.flipRounding()
            Float32.fusedMultiplyAdd(env, f1, f2, f3).negate()
        }

        val FNMSUBS = FusedFloat(
            "fnmsub.s f1, f2, f3, f4",
            "Fused Negate Multiply Subtract: Assigns -(f2*f3-f4) to f1",
            "10",
        ) { env, f1, f2, f3 ->
            env.flipRounding()
            Float32.fusedMultiplyAdd(env, f1, f2, f3.negate()).negate()
        }
    }
}
