package rars.riscv

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import rars.ProgramStatement
import rars.exceptions.ExceptionReason
import rars.exceptions.SimulationError
import rars.jsoftfloat.Environment
import rars.jsoftfloat.RoundingMode
import rars.jsoftfloat.types.Float32
import rars.jsoftfloat.types.Float64
import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile

fun CSRegisterFile.setfflags(environment: Environment): Either<SimulationError, Unit> =
    either {
        val fflags = listOf(
            environment.inexact to 1,
            environment.underflow to 2,
            environment.overflow to 4,
            environment.divByZero to 8,
            environment.invalid to 16
        ).filter { it.first }.sumOf { it.second }
        if (fflags != 0) {
            updateRegisterByName("fflags", getLongValue("fflags")!! or fflags.toLong()).bind()
        }
    }

fun CSRegisterFile.getRoundingMode(
    rmValue: Int,
    statement: ProgramStatement
): Either<SimulationError, RoundingMode> {
    val frm = getIntValue("frm")!!
    val rm = if (rmValue == 7) {
        frm
    } else {
        rmValue
    }
    return when (rm) {
        0 -> RoundingMode.EVEN.right() // RNE
        1 -> RoundingMode.ZERO.right() // RTZ
        2 -> RoundingMode.MIN.right()  // RDN
        3 -> RoundingMode.MAX.right()  // RUP
        4 -> RoundingMode.AWAY.right() // RMM
        else -> SimulationError.create(
            statement,
            "Invalid rounding mode. RM = $rmValue and frm = $frm",
            ExceptionReason.OTHER
        ).left()
    }
}

fun FloatingPointRegisterFile.getFloat32(num: Int) = Float32(getIntValue(num)!!)

fun FloatingPointRegisterFile.getFloat64(num: Int) = Float64(getLongValue(num)!!)
