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
import rars.jsoftfloat.types.Floating
import rars.riscv.hardware.registerFiles.CSRegisterFile
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile
import java.math.BigInteger

internal fun CSRegisterFile.setfflags(environment: Environment): Either<SimulationError, Unit> =
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

internal fun CSRegisterFile.getRoundingMode(
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

internal fun FloatingPointRegisterFile.getFloat32(num: Int) = Float32(getIntValue(num)!!)

internal fun FloatingPointRegisterFile.getFloat64(num: Int) = Float64(getLongValue(num)!!)
internal fun Int.lowerToULong(): ULong = this.toULong() and 0xFFFFFFFFu
internal fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)
internal fun ULong.toBigInteger(): BigInteger {
    val converted = this.toLong()
    return BigInteger.valueOf(converted).let {
        if (converted < 0) it.add(BigInteger.ONE.shiftLeft(64))
        else it
    }
}

// TODO: Create some kind of a factory interface for the Floating types to use here
internal fun <S : Floating<S>, D : Floating<D>> convert(
    toconvert: D,
    constructor: S,
    environment: Environment
): S = when {
    toconvert.isInfinite -> if (toconvert.isSignMinus) constructor.NegativeInfinity() else constructor.Infinity()
    toconvert.isZero -> if (toconvert.isSignMinus) constructor.NegativeZero() else constructor.Zero()
    toconvert.isNaN -> constructor.NaN()
    else -> constructor.fromExactFloat(toconvert.toExactFloat(), environment)
}
