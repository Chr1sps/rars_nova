package rars.ksoftfloat.operations

import rars.ksoftfloat.Environment
import rars.ksoftfloat.RoundingMode
import rars.ksoftfloat.types.Floating
import rars.ksoftfloat.types.FloatingFactory

fun <T : Floating<T>> FloatingFactory<T>.add(environment: Environment, a: T, b: T): T {

    // TODO: handle signalling correctly

    // Section 6.2
    if (a.isNaN) return a
    if (b.isNaN) return b

    // Section 6.1 and 7.2
    if (a.isInfinite) {
        if (b.isInfinite && (b.isSignMinus != a.isSignMinus)) {
            environment.invalid = true
            return NaN // inf - inf is undefined
        } else {
            return a
        }
    } else if (b.isInfinite) {
        return b
    }

    // Section 6.3
    if (a.isZero) {
        return if (b.isZero) {
            if (a.isSignMinus == b.isSignMinus) {
                a // They are the same, just pick one
            } else {
                // Explicitly stated in the spec
                if (environment.mode == RoundingMode.MIN) negativeZero else zero
            }
        } else {
            b
        }
    } else if (b.isZero) {
        return a
    }

    val out = a.toExactFloat() + b.toExactFloat()
    // Check to see if it was x + (-x)
    return if (out.isZero) {
        if (environment.mode == RoundingMode.MIN) negativeZero else zero
    } else
        fromExactFloat(environment, out)
}

fun <T : Floating<T>> FloatingFactory<T>.subtract(environment: Environment, a: T, b: T): T {
    // TODO: handle signalling correctly

    // Section 6.2

    if (a.isNaN) {
        return a
    }
    if (b.isNaN) {
        return b
    }

    // After this it is equivalent to adding a negative
    return add(environment, a, b.negate())
}

fun <T : Floating<T>> FloatingFactory<T>.multiply(environment: Environment, a: T, b: T): T {

    // TODO: handle signalling correctly

    // Section 6.2
    if (a.isNaN) {
        return a
    }
    if (b.isNaN) {
        return b
    }

    // Section 7.2
    if ((a.isZero && b.isInfinite) || (b.isZero && a.isInfinite)) {
        environment.invalid = true
        return NaN
    }

    // Section 6.1
    if (a.isInfinite || b.isInfinite) {
        return if (a.isSignMinus == b.isSignMinus) infinity else negativeInfinity
    }

    if (a.isZero || b.isZero) {
        return if (a.isSignMinus == b.isSignMinus) zero else negativeZero
    }

    return fromExactFloat(environment, a.toExactFloat().times(b.toExactFloat()))
}

fun <T : Floating<T>> FloatingFactory<T>.squareRoot(environment: Environment, a: T): T {
    // TODO: handle signalling correctly

    // Section 6.2
    if (a.isNaN) {
        return a
    }

    // Section 6.3 or Section 5.4.1
    if (a.isZero) {
        return a
    }

    // Section 7.2
    if (a.isSignMinus) {
        environment.invalid = true
        return NaN
    }

    // Section 6.1
    if (a.isInfinite) {
        return a
    }

    return fromExactFloat(environment, a.toExactFloat().squareRoot(maxPrecision))
}

fun <T : Floating<T>> FloatingFactory<T>.fusedMultiplyAdd(environment: Environment, a: T, b: T, c: T): T {

    // TODO: handle signalling correctly

    // Section 6.2
    if (a.isNaN) {
        return a
    }
    if (b.isNaN) {
        return b
    }
    // This behaviour is implementation defined - Section 7.2
    if (c.isNaN) {
        return c
    }

    // Section 7.2
    if ((a.isZero && b.isInfinite) || (b.isZero && a.isInfinite)) {
        environment.invalid = true
        return NaN
    }

    // Section 6.1
    if (a.isInfinite || b.isInfinite) {
        return add(
            environment, if (a.isSignMinus == b.isSignMinus)
                infinity
            else
                negativeInfinity, c
        )
    }

    if (a.isZero || b.isZero) {
        return add(
            environment,
            if (a.isSignMinus == b.isSignMinus) zero else negativeZero,
            c
        )
    }

    val multiplied = a.toExactFloat() * b.toExactFloat()

    return fromExactFloat(environment, multiplied + c.toExactFloat())
}

fun <T : Floating<T>> FloatingFactory<T>.divide(environment: Environment, a: T, b: T): T {
    // TODO: handle signalling correctly

    // Section 6.2
    if (a.isNaN) {
        return a
    }
    if (b.isNaN) {
        return b
    }

    // Section 7.2
    if ((a.isZero && b.isZero) || (a.isInfinite && b.isInfinite)) {
        environment.invalid = true
        return NaN
    }

    // Section 6.1
    if (a.isInfinite) {
        return if (a.isSignMinus == b.isSignMinus) infinity else negativeInfinity
    }

    if (b.isInfinite || a.isZero) {
        return if (a.isSignMinus == b.isSignMinus) zero else negativeZero
    }

    // Section 7.3
    if (b.isZero) {
        environment.divByZero = true
        return if (a.isSignMinus == b.isSignMinus) infinity else negativeInfinity
    }

    assert(a.isFinite && b.isFinite) { "Both should definitely be finite by this point" }

    // TODO: in tie cases round away from zero despite rounding mode unless actually
    // precise
    return fromExactFloat(
        environment,
        a.toExactFloat().divide(
            b.toExactFloat(),
            maxPrecision
        )
    )
}

