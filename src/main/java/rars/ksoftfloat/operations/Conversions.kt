package rars.ksoftfloat.operations

import rars.ksoftfloat.Environment
import rars.ksoftfloat.internal.ExactFloat
import rars.ksoftfloat.types.Floating
import rars.ksoftfloat.types.FloatingFactory
import java.math.BigInteger

fun <T : Floating<T>> T.roundToIntegral(
    env: Environment,
    factory: FloatingFactory<T>
): T {
    // Section 5.9 and 7.2
    if (isNaN) {
        // TODO: signal invalid operation
        return this
    }
    if (isInfinite) {
        // TODO: handle correctly
        return this
    }
    if (isZero) {
        return this
    }
    return factory.fromExactFloat(env, toExactFloat().roundToIntegral(env))
}

private fun <T : Floating<T>> convertToIntegral(
    f: T, max: BigInteger, min: BigInteger,
    env: Environment, quiet: Boolean
): BigInteger {
    // Section 5.9 and 7.2
    if (f.isNaN) {
        env.invalid = true
        return max
    }

    if (f.isInfinite) {
        env.invalid = true
        return if (f.isSignMinus) min else max
    }

    val copy = Environment()
    copy.mode = env.mode
    val rounded: BigInteger
    if (f.isZero) {
        rounded = BigInteger.ZERO
    } else {
        rounded = f.toExactFloat().toBigInteger(env)
    }

    // Section 5.8
    if (rounded > max || rounded < min) {
        env.invalid = true
    } else if (!quiet && copy.inexact) {
        env.inexact = true
    }
    return rounded.min(max).max(min) // clamp rounded to between max and min
}

fun <T : Floating<T>> T.toInt(env: Environment) = toInt(env, false)

fun <T : Floating<T>> T.toInt(
    env: Environment,
    quiet: Boolean
): Int {
    val rounded = convertToIntegral(
        this, BigInteger.valueOf(Int.Companion.MAX_VALUE.toLong()),
        BigInteger.valueOf(Int.Companion.MIN_VALUE.toLong()), env, quiet
    )
    return rounded.intValueExact()
}

fun <T : Floating<T>> T.toUInt(
    env: Environment,
    quiet: Boolean
): UInt {
    val rounded = convertToIntegral(
        this,
        BigInteger.valueOf(0xFFFFFFFFL),
        BigInteger.ZERO,
        env,
        quiet
    )
    return (rounded.longValueExact() and 0xFFFFFFFFL).toUInt()
}

fun <T : Floating<T>> T.toLong(
    env: Environment,
    quiet: Boolean
): Long {
    val rounded = convertToIntegral(
        this, BigInteger.valueOf(Long.Companion.MAX_VALUE),
        BigInteger.valueOf(Long.Companion.MIN_VALUE), env, quiet
    )
    return rounded.longValueExact()
}

fun <T : Floating<T>> T.toULong(
    env: Environment,
    quiet: Boolean
): ULong {
    val rounded = convertToIntegral(
        this, BigInteger.valueOf(-1).add(BigInteger.ONE.shiftLeft(64)),
        BigInteger.ZERO, env, quiet
    )
    return rounded.toULong()
}

fun <T : Floating<T>> FloatingFactory<T>.fromBigInteger(
    bigInt: BigInteger,
    env: Environment
): T = if (bigInt == BigInteger.ZERO) {
    zero
} else fromExactFloat(env, ExactFloat(bigInt))


private fun BigInteger.toULong(): ULong = this.toLong().toULong()