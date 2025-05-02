package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.events.SimulationEvent
import rars.ksoftfloat.Environment
import rars.ksoftfloat.operations.fusedMultiplyAdd
import rars.ksoftfloat.types.Float64
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.riscv.setfflags
import rars.simulator.SimulationContext
import rars.util.flipRounding

class FusedDouble(
    usage: String,
    description: String,
    op: String,
    private val compute: (Environment, Float64, Float64, Float64) -> Float64
) : BasicInstruction(
    "$usage, dyn",
    description,
    BasicInstructionFormat.R4_FORMAT,
    "qqqqq 01 ttttt sssss ppp fffff 100${op}11"
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
                Float64(fpRegisterFile.getLong(statement.getOperand(1))!!),
                Float64(fpRegisterFile.getLong(statement.getOperand(2))!!),
                Float64(fpRegisterFile.getLong(statement.getOperand(3))!!),
            )
            csrRegisterFile.setfflags(environment).bind()
            fpRegisterFile.updateRegisterByNumber(
                statement.getOperand(0),
                result.bits
            ).bind()
        }

    companion object {
        val FMADD = FusedDouble(
            "fmadd.d f1, f2, f3, f4",
            "Fused Multiply Add (64 bit): Assigns f2*f3+f4 to f1",
            "00",
            Float64::fusedMultiplyAdd
        )

        val FMSUBD = FusedDouble(
            "fmsub.d f1, f2, f3, f4",
            "Fused Multiply Subtract (64 bit): Assigns f2*f3-f4 to f1",
            "01",
        ) { env, f1, f2, f3 ->
            Float64.fusedMultiplyAdd(
                env,
                f1,
                f2,
                f3.negate()
            )
        }

        val FNMADDD = FusedDouble(
            "fnmadd.d f1, f2, f3, f4",
            "Fused Negate Multiply Add (64 bit): Assigns -(f2*f3+f4) to f1",
            "11"
        ) { env, f1, f2, f3 ->
            // TODO: test if this is the right behaviour
            env.flipRounding()
            Float64.fusedMultiplyAdd(env, f1, f2, f3).negate()
        }

        val FNMSUBD = FusedDouble(
            "fnmsub.d f1, f2, f3, f4",
            "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1",
            "10"
        ) { env, f1, f2, f3 ->
            env.flipRounding()
            Float64.fusedMultiplyAdd(env, f1, f2, f3.negate()).negate()
        }
    }
}

