package rars.ksoftfloat.operations

import rars.ksoftfloat.Environment
import rars.ksoftfloat.types.Floating
import rars.ksoftfloat.types.FloatingFactory


private fun <T : Floating<T>> compareNoNAN(a: T, b: T): Int {
    if (a.isZero) {
        return if (b.isZero) {
            0
        } else {
            if (b.isSignMinus) 1 else -1
        }
    }
    if (b.isZero) {
        return if (a.isSignMinus) -1 else 1
    }
    if (a.isInfinite) {
        return if (b.isInfinite && a.isSignMinus == b.isSignMinus) {
            0
        } else {
            if (a.isSignMinus) 1 else -1
        }
    }
    if (b.isInfinite) {
        return if (b.isSignMinus) 1 else -1
    }
    return a.toExactFloat().compareTo(b.toExactFloat())
}

private fun <T : Floating<T>> nonNaNmin(a: T, b: T): T {
    // If signs are different it is easy
    // Also explicitly handles -0 vs +0
    if (a.isSignMinus != b.isSignMinus) {
        return (if (a.isSignMinus) a else b)
    }
    // Handle the infinite cases first
    if (a.isInfinite || b.isInfinite) {
        return if (a.isInfinite == a.isSignMinus) {
            a
        } else {
            b
        }
    }
    return if (compareNoNAN(a, b) <= 0) {
        a
    } else {
        b
    }
}

private fun <T : Floating<T>> FloatingFactory<T>.handleNaN(
    a: T,
    b: T
): T? {
    // Section 5.3.1
    if (a.isNaN) {
        return if (b.isNaN) {
            NaN // Canonicalize in the case of two NaNs
        } else {
            b
        }
    }
    if (b.isNaN) {
        return a
    }
    return null
}

private fun <T : Floating<T>> FloatingFactory<T>.handleNaNNumber(
    environment: Environment,
    a: T,
    b: T
): T? {
    if (a.isSignalling || b.isSignalling) {
        environment.invalid = true
    }
    // I think this handles the remaining cases of NaNs
    return handleNaN(a, b)
}

// minimum and minimumNumber are from the 201x revision
fun <T : Floating<T>> FloatingFactory<T>.min(
    a: T,
    b: T,
): T {
    val tmp = handleNaN(a, b)
    if (tmp != null) {
        return tmp
    }
    return nonNaNmin(a, b)
}

fun <T : Floating<T>> FloatingFactory<T>.max(
    a: T,
    b: T
): T {
    var tmp = handleNaN(a, b)
    if (tmp != null) {
        return tmp
    }
    tmp = nonNaNmin(a, b)
    return if (a === tmp) b else a // flip for max rather than min
}

// Literally the same code as above, but with a different NaN handler
fun <T : Floating<T>> FloatingFactory<T>.min(
    environment: Environment,
    a: T,
    b: T
): T {
    val tmp = this.handleNaNNumber(environment, a, b)
    if (tmp != null) {
        return tmp
    }
    return nonNaNmin(a, b)
}

fun <T : Floating<T>> FloatingFactory<T>.max(
    environment: Environment,
    a: T,
    b: T
): T {
    var tmp = this.handleNaNNumber(environment, a, b)
    if (tmp != null) {
        return tmp
    }
    tmp = nonNaNmin(a, b)
    return if (a === tmp) b else a // flip for max rather than min
}

// Difference from minimumNumber explained by
// https://freenode.logbot.info/riscv/20191012
// > (TLDR: minNum(a, sNaN) == minNum(sNaN, a) == qNaN, whereas minimumNumber(a,
// sNaN) == minimumNumber(sNaN, a) == a, where a is not NaN)
fun <T : Floating<T>> FloatingFactory<T>.minNum(
    environment: Environment,
    a: T,
    b: T
): T {
    if (a.isSignalling || b.isSignalling) {
        environment.invalid = true
        return NaN
    }
    val tmp = handleNaN(a, b)
    if (tmp != null) {
        return tmp
    }
    return nonNaNmin(a, b)
}

fun <T : Floating<T>> FloatingFactory<T>.maxNum(
    environment: Environment,
    a: T,
    b: T
): T {
    if (a.isSignalling || b.isSignalling) {
        environment.invalid = true
        return NaN
    }
    var tmp = handleNaN(a, b)
    if (tmp != null) {
        return tmp
    }
    tmp = nonNaNmin(a, b)
    return if (a === tmp) b else a // flip for max rather than min
}

// All compares covered in Section 5.11
fun <T : Floating<T>> Environment.compareQuietEqual(
    a: T,
    b: T
): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    if (a.isNaN || b.isNaN) {
        return false
    }
    return compareNoNAN(a, b) == 0
}

fun <T : Floating<T>> Environment.equalSignaling(
    a: T,
    b: T
): Boolean {
    if (a.isNaN || b.isNaN) {
        invalid = true
    }
    return compareQuietEqual(a, b)
}

fun <T : Floating<T>> Environment.compareQuietLessThan(
    a: T,
    b: T
): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    if (a.isNaN || b.isNaN) {
        return false
    }
    return compareNoNAN(a, b) < 0
}

fun <T : Floating<T>> Environment.compareSignalingLessThan(
    a: T,
    b: T
): Boolean {
    if (a.isNaN || b.isNaN) {
        invalid = true
    }
    return compareQuietLessThan(a, b)
}


fun <T : Floating<T>> Environment.compareQuietLessThanEqual(
    a: T,
    b: T
): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    if (a.isNaN || b.isNaN) {
        return false
    }
    return compareNoNAN(a, b) <= 0
}


fun <T : Floating<T>> Environment.compareSignalingLessThanEqual(
    a: T,
    b: T
): Boolean {
    if (a.isNaN || b.isNaN) {
        invalid = true
    }
    return compareQuietLessThanEqual(a, b)
}


fun <T : Floating<T>> Environment.compareQuietGreaterThan(
    a: T,
    b: T
): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    if (a.isNaN || b.isNaN) {
        return false
    }
    return compareNoNAN(a, b) > 0
}


fun <T : Floating<T>> Environment.compareSignalingGreaterThan(
    a: T,
    b: T
): Boolean {
    if (a.isNaN || b.isNaN) {
        invalid = true
    }
    return compareQuietGreaterThan(a, b)
}


fun <T : Floating<T>> Environment.compareQuietGreaterThanEqual(
    a: T,
    b: T
): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    if (a.isNaN || b.isNaN) {
        return false
    }
    return compareNoNAN(a, b) >= 0
}


fun <T : Floating<T>> Environment.compareSignalingGreaterThanEqual(
    a: T,
    b: T
): Boolean {
    if (a.isNaN || b.isNaN) {
        invalid = true
    }
    return this.compareQuietGreaterThanEqual(a, b)
}


fun <T : Floating<T>> Environment.compareQuietUnordered(a: T, b: T): Boolean {
    if (a.isSignalling || b.isSignalling) {
        invalid = true
    }
    return a.isNaN || b.isNaN
}
