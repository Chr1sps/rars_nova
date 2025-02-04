package rars.riscv.instructions

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.jsoftfloat.Environment
import rars.jsoftfloat.operations.Arithmetic
import rars.jsoftfloat.types.Float64
import rars.riscv.BasicInstruction
import rars.riscv.BasicInstructionFormat
import rars.simulator.SimulationContext
import rars.util.flipRounding

class FusedDouble(
    usage: String,
    description: String,
    op: String,
    private val compute: (Float64, Float64, Float64, Environment) -> Float64
) : BasicInstruction(
    "$usage, dyn",
    description,
    BasicInstructionFormat.R4_FORMAT,
    "qqqqq 01 ttttt sssss ppp fffff 100${op}11"
) {
    override fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit> = either {
        val environment = Environment()
        environment.mode = Floating.FloatingUtils.getRoundingMode(
            statement.getOperand(4),
            statement,
            csrRegisterFile
        ).bind()
        val result = compute(
            Float64(fpRegisterFile.getLongValue(statement.getOperand(1))!!),
            Float64(fpRegisterFile.getLongValue(statement.getOperand(2))!!),
            Float64(fpRegisterFile.getLongValue(statement.getOperand(3))!!),
            environment
        )
        Floating.FloatingUtils.setfflags(csrRegisterFile, environment).bind()
        fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result.bits).bind()
    }

    companion object {
        @JvmField
        val FMADD = FusedDouble(
            "fmadd.d f1, f2, f3, f4",
            "Fused Multiply Add (64 bit): Assigns f2*f3+f4 to f1",
            "00"
        ) { f1, f2, f3, env -> Arithmetic.fusedMultiplyAdd(f1, f2, f3, env) }

        @JvmField
        val FMSUBD = FusedDouble(
            "fmsub.d f1, f2, f3, f4",
            "Fused Multiply Subtract (64 bit): Assigns f2*f3-f4 to f1",
            "01"
        ) { f1, f2, f3, env -> Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), env) }

        @JvmField
        val FNMADDD = FusedDouble(
            "fnmadd.d f1, f2, f3, f4",
            "Fused Negate Multiply Add (64 bit): Assigns -(f2*f3+f4) to f1",
            "11"
        ) { f1, f2, f3, env ->
            // TODO: test if this is the right behaviour
            env.flipRounding()
            Arithmetic.fusedMultiplyAdd(f1, f2, f3, env).negate()
        }

        @JvmField
        val FNMSUBD = FusedDouble(
            "fnmsub.d f1, f2, f3, f4",
            "Fused Negated Multiply Subatract: Assigns -(f2*f3-f4) to f1",
            "10"
        ) { f1, f2, f3, env ->
            env.flipRounding()
            Arithmetic.fusedMultiplyAdd(f1, f2, f3.negate(), env).negate()
        }
    }
}

