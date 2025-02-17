package rars.ksoftfloat.types

import rars.ksoftfloat.Environment
import rars.ksoftfloat.internal.ExactFloat

interface Floating<T : Floating<T>> {
    val isSignMinus: Boolean
    val isInfinite: Boolean
    val isFinite get() = isZero || isNormal || isSubnormal
    val isNormal: Boolean
    val isSubnormal: Boolean
    val isNaN: Boolean
    val isSignalling: Boolean
    val isZero: Boolean

    fun toExactFloat(): ExactFloat
    fun negate(): T
}

interface FloatingFactory<T : Floating<T>> {
    @Suppress("PropertyName")
    val NaN: T
    val zero: T
    val negativeZero: T
    val infinity: T
    val negativeInfinity: T
    fun fromExactFloat(env: Environment, value: ExactFloat): T
    val maxPrecision: Int
}
