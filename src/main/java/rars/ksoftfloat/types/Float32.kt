package rars.ksoftfloat.types

import rars.ksoftfloat.Environment
import rars.ksoftfloat.RoundingMode
import rars.ksoftfloat.internal.ExactFloat
import java.math.BigInteger
import kotlin.math.absoluteValue

/**
 * Represents the Binary32 format
 */
class Float32(val bits: Int) : Floating<Float32> {
    constructor(
        sign: Boolean,
        exponent: Int,
        significand: Int
    ) : this((if (sign) -0x80000000 else 0) or (((exponent + 127) and 0xFF) shl 23) or (significand and 0x007FFFFF))

    val exponent: Int get() = ((this.bits ushr 23) and 0xFF) - 127

    // Flip the sign bit
    override fun negate(): Float32 = Float32(this.bits xor Int.MIN_VALUE)

    fun abs(): Float32 = Float32(this.bits and 0x7FFFFFFF)

    fun copySign(signToTake: Float32): Float32 =
        Float32((this.bits and 0x7FFFFFFF) or (signToTake.bits and Int.MIN_VALUE))

    override val isSignMinus get() = (this.bits ushr 31) == 1

    override val isInfinite get() = this.exponent == 128 && (this.bits and 0x007FFFFF) == 0

    override val isNormal get() = this.exponent != -127 && this.exponent != 128

    override val isSubnormal get() = this.exponent == -127 && !this.isZero

    // Section 6.2.1
    override val isNaN get() = this.exponent == 128 && !this.isInfinite

    override val isSignalling
        get() = if (!this.isNaN) {
            false
        } else (this.bits and 0x400000) == 0

    override val isZero get() = this.bits == 0 || this.bits == -0x80000000

    override fun toExactFloat(): ExactFloat {
        assert(!this.isInfinite) { "Infinity is not exact" }
        assert(!this.isNaN) { "NaNs are not exact" }
        assert(!this.isZero) { "Zeros should be handled explicitly" }

        val exponent: Int
        val significand: BigInteger
        when {
            this.isZero -> {
                exponent = 0
                significand = BigInteger.ZERO
            }
            this.isNormal -> {
                exponent = this.exponent - 23
                // Add back the implied one
                significand = BigInteger.valueOf(((this.bits and 0x007FFFFF) + 0x00800000).toLong())
            }
            this.isSubnormal -> {
                exponent = this.exponent - 22
                significand = BigInteger.valueOf((this.bits and 0x007FFFFF).toLong())
            }
            else -> error("This should not be reachable")
        }
        return ExactFloat(this.isSignMinus, exponent, significand)
    }

    companion object : FloatingFactory<Float32> {
        override val NaN: Float32 = Float32(0x7FC00000)
        override val zero = Float32(0)
        override val negativeZero = Float32(-0x80000000)
        override val infinity = Float32(0x7F800000)
        override val negativeInfinity = Float32(-0x800000)
        override fun fromExactFloat(
            env: Environment,
            value: ExactFloat
        ): Float32 {
            var ef = value
            if (ef.isZero) {
                return if (ef.sign) negativeZero else zero
            }
            ef = ef.normalize()
            val normalizedExponent = ef.exponent + ef.significand.bitLength()

            // Used to calculate how to round at the end
            val awayZero: Float32
            val towardsZero: Float32
            val roundedBits: BigInteger
            val bitsToRound: Int

            if (normalizedExponent <= MINEXP + 1) {
                // Subnormal

                if (ef.exponent > MINEXP - SIGBITS) {
                    assert(ef.significand.bitLength() <= SIGBITS) { "Its actually normal" }
                    return Float32(
                        ef.sign, MINEXP,
                        ef.significand.shiftLeft(-(MINEXP - SIGBITS + 1) + ef.exponent).intValueExact()
                    )
                }

                env.inexact = true
                env.underflow = true // Section 7.5
                bitsToRound = (MINEXP - SIGBITS + 1) - ef.exponent
                val mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound)
                roundedBits = ef.significand.subtract(mainBits)

                towardsZero = Float32(ef.sign, MINEXP, ef.significand.shiftRight(bitsToRound).intValueExact())
                val upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1))
                if (upBits.testBit(0) || upBits.bitLength() <= SIGBITS) {
                    assert(upBits.bitLength() <= SIGBITS)
                    awayZero = Float32(ef.sign, MINEXP, upBits.intValueExact())
                } else {
                    awayZero = Float32(ef.sign, MINEXP + 1, upBits.intValueExact() and SIGMASK)
                }
            } else if (normalizedExponent > MAXEXP) {
                // Section 7.4
                env.overflow = true
                env.inexact = true
                return when (env.mode) {
                    RoundingMode.ZERO -> Float32(ef.sign, MAXEXP - 1, -1) // Largest finite number
                    RoundingMode.MIN, RoundingMode.MAX -> if (ef.sign != (env.mode == RoundingMode.MAX)) {
                        if (ef.sign) negativeInfinity else infinity
                    } else {
                        Float32(ef.sign, MAXEXP - 1, -1) // Largest finite number
                    }

                    RoundingMode.AWAY, RoundingMode.EVEN -> if (ef.sign) negativeInfinity else infinity
                }
                assert(false) { "Not reachable" }
                return if (ef.sign) negativeInfinity else infinity
            } else {
                if (ef.significand.bitLength() <= (SIGBITS + 1)) {
                    // No rounding needed
                    val bitCount = ef.exponent + ef.significand.bitLength() - 1
                    assert(bitCount > MINEXP) { "Its actually subnormal" }

                    return Float32(
                        ef.sign, bitCount,
                        ef.significand.shiftLeft((SIGBITS + 1) - ef.significand.bitLength())
                            .intValueExact() and SIGMASK
                    )
                }
                env.inexact = true
                bitsToRound = ef.significand.bitLength() - (SIGBITS + 1)
                val mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound)
                roundedBits = ef.significand.subtract(mainBits)

                val upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1))

                towardsZero = Float32(
                    ef.sign, ef.exponent + SIGBITS + bitsToRound,
                    ef.significand.shiftRight(bitsToRound).intValueExact() and SIGMASK
                )
                awayZero = if (upBits.testBit(0) || upBits.bitLength() <= SIGBITS + 1) {
                    Float32(
                        ef.sign,
                        ef.exponent + SIGBITS + bitsToRound,
                        upBits.intValueExact() and SIGMASK
                    )
                } else {
                    Float32(
                        ef.sign, ef.exponent + (SIGBITS + 1) + bitsToRound,
                        upBits.shiftRight(1).intValueExact() and SIGMASK
                    )
                }
            }

            // Either round towards or away from zero based on rounding mode
            when (env.mode) {
                RoundingMode.ZERO -> return towardsZero
                RoundingMode.MAX, RoundingMode.MIN -> return if (ef.sign != (env.mode == RoundingMode.MAX)) {
                    awayZero
                } else {
                    towardsZero
                }

                else -> {}
            }

            // See which result is closer to the non-rounded version
            return if (roundedBits == BigInteger.ONE.shiftLeft(bitsToRound - 1)) {
                if (env.mode == RoundingMode.AWAY || (awayZero.bits and 1) == 0) {
                    awayZero
                } else {
                    towardsZero
                }
            } else if (roundedBits > BigInteger.ONE.shiftLeft(bitsToRound - 1)) {
                awayZero
            } else {
                towardsZero
            }
        }

        override val maxPrecision = 30

        private const val SIGBITS = 23
        private const val EXPBITS = 8
        private const val MAXEXP = 1 shl (EXPBITS - 1)
        private const val MINEXP = -(1 shl (EXPBITS - 1)) + 1
        private const val SIGMASK = (1 shl SIGBITS) - 1

        fun fromInteger(value: Int): Float32? {
            if (value == 0) return zero
            val sign = value < 0
            val absolute = value.absoluteValue
            var exponent = 0
            var significand = 0
            for (i in 30 downTo 0) {
                if (((absolute shr i) and 1) == 1) {
                    exponent = i + 127
                    significand = (absolute shl (32 - i)) ushr 9
                    break
                }
            }
            val bits = (if (sign) -0x80000000 else 0) or (exponent shl 23) or significand
            return Float32(bits)
        }
    }
}
