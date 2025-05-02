package rars.riscv

import arrow.core.Either
import arrow.core.raise.either
import rars.events.SimulationError
import rars.ksoftfloat.Environment
import rars.ksoftfloat.types.Float32
import rars.ksoftfloat.types.Float64
import rars.ksoftfloat.types.Floating
import rars.ksoftfloat.types.FloatingFactory
import rars.riscv.hardware.registerfiles.CSRegisterFile
import rars.riscv.hardware.registerfiles.FloatingPointRegisterFile
import java.math.BigInteger

internal fun CSRegisterFile.setfflags(
    environment: Environment
): Either<SimulationError, Unit> = either {
    val fflags = listOf(
        environment.inexact to 1,
        environment.underflow to 2,
        environment.overflow to 4,
        environment.divByZero to 8,
        environment.invalid to 16
    ).filter { it.first }.sumOf { it.second }
    if (fflags != 0) {
        updateRegisterByName(
            "fflags",
            getLong("fflags")!! or fflags.toLong()
        ).bind()
    }
}

//internal fun CSRegisterFile.getRoundingMode(
//    rmValue: Int,
//    statement: ProgramStatement
//): Either<SimulationError, RoundingMode> {
//    val frm = getInt("frm")!!
//    val rm = if (rmValue == 7) {
//        frm
//    } else {
//        rmValue
//    }
//    return when (rm) {
//        0 -> RoundingMode.EVEN.right() // RNE
//        1 -> RoundingMode.ZERO.right() // RTZ
//        2 -> RoundingMode.MIN.right()  // RDN
//        3 -> RoundingMode.MAX.right()  // RUP
//        4 -> RoundingMode.AWAY.right() // RMM
//        else -> SimulationError.create(
//            statement,
//            "Invalid rounding mode. RM = $rmValue and frm = $frm",
//            EventReason.OTHER
//        ).left()
//    }
//}

internal fun FloatingPointRegisterFile.getFloat32(num: Int) =
    Float32(getInt(num)!!)

internal fun FloatingPointRegisterFile.getFloat64(num: Int) =
    Float64(getLong(num)!!)

internal fun Int.lowerToULong(): ULong = this.toULong() and 0xFFFFFFFFu
internal fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)
internal fun ULong.toBigInteger(): BigInteger {
    val converted = this.toLong()
    return BigInteger.valueOf(converted).let {
        if (converted < 0) it.add(BigInteger.ONE.shiftLeft(64))
        else it
    }
}

internal fun <S : Floating<S>, D : Floating<D>> FloatingFactory<S>.convert(
    valueToConvert: D,
    environment: Environment
): S = when {
    valueToConvert.isInfinite -> if (valueToConvert.isSignMinus) negativeInfinity else infinity
    valueToConvert.isZero -> if (valueToConvert.isSignMinus) negativeZero else zero
    valueToConvert.isNaN -> NaN
    else -> fromExactFloat(environment, valueToConvert.toExactFloat())
}
