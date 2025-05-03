package rars.ieee754.types

import rars.ieee754.Environment
import rars.ieee754.RoundingMode
import rars.ieee754.internal.ExactFloat
import java.math.BigInteger

/** Represents the Binary64 format */
class Float64(val bits: Long) : Floating<Float64> {
    constructor(
        sign: Boolean,
        exponent: Int,
        significand: Long
    ) : this((if (sign) Long.MIN_VALUE else 0L) or (((exponent + 1023).toLong() and 0x7FFL) shl SIGBITS) or (significand and SIGMASK))

    val exponent: Int get() = ((this.bits ushr SIGBITS).toInt() and 0x7FF) - 1023

    override fun negate(): Float64 = Float64(this.bits xor Long.MIN_VALUE)

    fun abs(): Float64 = Float64(this.bits and 0x7FFFFFFFFFFFFFFFL)

    fun copySign(signToTake: Float64): Float64 =
        Float64((this.bits and 0x7FFFFFFFFFFFFFFFL) or (signToTake.bits and Long.MIN_VALUE))

    override val isSignMinus get() = (this.bits ushr 63) == 1L

    override val isInfinite get() = this.exponent == 1024 && (this.bits and SIGMASK) == 0L

    override val isNormal get() = this.exponent != -1023 && this.exponent != 1024

    override val isSubnormal get() = this.exponent == -1023 && !this.isZero

    override val isNaN get() = this.exponent == 1024 && !this.isInfinite

    override val isSignalling
        get() = if (!this.isNaN) {
            false
        } else (this.bits and 0x8000000000000L) == 0L

    override val isZero get() = this.bits == 0L || this.bits == Long.MIN_VALUE

    override fun toExactFloat(): ExactFloat {
        assert(!this.isInfinite) { "Infinity is not exact" }
        assert(!this.isNaN) { "NaNs are not exact" }
        assert(!this.isZero) { "Zeros should be handled explicitly" }

        val sign = this.isSignMinus
        val exponent: Int
        val significand: BigInteger
        if (this.isZero) {
            exponent = 0
            significand = BigInteger.ZERO
        } else if (this.isNormal) {
            exponent = this.exponent - SIGBITS
            significand =
                BigInteger.valueOf((this.bits and SIGMASK) + (SIGMASK + 1)) // Add back the implied one
        } else if (this.isSubnormal) {
            exponent = this.exponent - (SIGBITS - 1)
            significand = BigInteger.valueOf(this.bits and SIGMASK)
        } else error("This should not be reachable")
        return ExactFloat(sign, exponent, significand)
    }

    companion object : FloatingFactory<Float64> {
        override val NaN = Float64(0x7FF8000000000000L)
        override val zero = Float64(0)
        override val negativeZero = Float64(Long.MIN_VALUE)
        override val infinity = Float64(0x7FF0000000000000L)
        override val negativeInfinity = Float64(-0x10000000000000L)

        override fun fromExactFloat(
            env: Environment,
            value: ExactFloat
        ): Float64 {
            if (value.isZero) {
                return if (value.sign) negativeZero else zero
            }
            val normalized = value.normalize()
            val normalizedExponent =
                normalized.exponent + normalized.significand.bitLength()

            // Used to calculate how to round at the end

            val awayZero: Float64
            val towardsZero: Float64
            val roundedBits: BigInteger
            val bitsToRound: Int

            if (normalizedExponent <= MINEXP + 1) {
                // Subnormal

                if (normalized.exponent > MINEXP - SIGBITS) {
                    assert(normalized.significand.bitLength() <= SIGBITS) { "Its actually normal" }
                    return Float64(
                        normalized.sign, MINEXP,
                        normalized.significand.shl(-(MINEXP - SIGBITS + 1) + normalized.exponent)
                            .toLong()
                    )
                }
                env.inexact = true
                env.underflow = true // Section 7.5
                bitsToRound = (MINEXP - SIGBITS + 1) - normalized.exponent
                val mainBits =
                    normalized.significand.shr(bitsToRound).shl(bitsToRound)
                roundedBits = normalized.significand - mainBits
                towardsZero = Float64(
                    normalized.sign,
                    MINEXP,
                    normalized.significand.shr(bitsToRound).toLong()
                )
                val upBits =
                    normalized.significand.shr(bitsToRound) + BigInteger.ONE
                awayZero =
                    if (upBits.testBit(0) || upBits.bitLength() <= SIGBITS) {
                        assert(upBits.bitLength() <= SIGBITS)
                        Float64(normalized.sign, MINEXP, upBits.toLong())
                    } else {
                        Float64(
                            normalized.sign,
                            MINEXP + 1,
                            upBits.toLong() and SIGMASK
                        )
                    }
            } else if (normalizedExponent > MAXEXP) {
                // Section 7.4
                env.overflow = true
                env.inexact = true
                return when (env.mode) {
                    RoundingMode.ZERO -> Float64(
                        normalized.sign,
                        MAXEXP - 1,
                        -1
                    ) // Largest finite number
                    RoundingMode.MIN, RoundingMode.MAX -> if (normalized.sign != (env.mode == RoundingMode.MAX)) {
                        if (normalized.sign) negativeInfinity else infinity
                    } else {
                        Float64(
                            normalized.sign,
                            MAXEXP - 1,
                            -1
                        ) // Largest finite number
                    }
                    RoundingMode.AWAY, RoundingMode.EVEN -> if (normalized.sign) negativeInfinity else infinity
                }
            } else {

                if (normalized.significand.bitLength() <= (SIGBITS + 1)) {
                    // No rounding needed
                    val tmp =
                        normalized.exponent + normalized.significand.bitLength() - 1
                    assert(tmp > MINEXP) { "Its actually subnormal" }

                    return Float64(
                        normalized.sign,
                        tmp,
                        normalized.significand.shl(
                            (SIGBITS + 1) - normalized.significand.bitLength()
                        ).toLong() and SIGMASK
                    )
                }
                env.inexact = true
                bitsToRound = normalized.significand.bitLength() - (SIGBITS + 1)
                val mainBits =
                    normalized.significand.shr(bitsToRound).shl(bitsToRound)
                roundedBits = normalized.significand - mainBits

                val upBits =
                    normalized.significand.shr(bitsToRound) + BigInteger.ONE

                towardsZero = Float64(
                    normalized.sign,
                    normalized.exponent + SIGBITS + bitsToRound,
                    normalized.significand.shr(bitsToRound).toLong() and SIGMASK
                )

                awayZero =
                    if (upBits.testBit(0) || upBits.bitLength() <= SIGBITS + 1) {
                        Float64(
                            normalized.sign,
                            normalized.exponent + SIGBITS + bitsToRound,
                            upBits.toLong() and SIGMASK
                        )
                    } else {
                        Float64(
                            normalized.sign,
                            normalized.exponent + (SIGBITS + 1) + bitsToRound,
                            upBits.shr(1).toLong() and SIGMASK
                        )
                    }
            }

            // Either round towards or away from zero based on rounding mode
            when (env.mode) {
                RoundingMode.ZERO -> return towardsZero
                RoundingMode.MAX, RoundingMode.MIN -> return if (normalized.sign != (env.mode == RoundingMode.MAX)) {
                    if (normalized.sign) negativeInfinity else infinity
                } else {
                    towardsZero
                }
                else -> Unit
            }

            // See which result is closer to the non-rounded version
            return if (roundedBits == BigInteger.ONE.shl(bitsToRound - 1)) {
                if (env.mode == RoundingMode.AWAY || (awayZero.bits and 1) == 0L) {
                    awayZero
                } else {
                    towardsZero
                }
            } else if (roundedBits > BigInteger.ONE.shl(bitsToRound - 1)) {
                awayZero
            } else {
                towardsZero
            }
        }

        override val maxPrecision = 60

        private const val SIGBITS = 52
        private const val EXPBITS = 11
        private const val MAXEXP = 1 shl (EXPBITS - 1)
        private const val MINEXP = -(1 shl (EXPBITS - 1)) + 1
        private const val SIGMASK = (1L shl SIGBITS) - 1
    }
}
