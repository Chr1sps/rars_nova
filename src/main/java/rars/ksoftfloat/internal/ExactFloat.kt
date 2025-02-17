package rars.ksoftfloat.internal

import rars.ksoftfloat.Environment
import rars.ksoftfloat.RoundingMode
import java.math.BigInteger
import kotlin.math.min

/**
 * A helper type to generalize floating point exact operations. This helps to
 * reduce the extra rounding code into just
 * one place. It isn't able to refer to every exact float though. 1/3 is not
 * representable as it requires infinite
 * precision, but any finite significand is representable.
 *
 *
 * Square Root and Division cannot really use this because they cannot avoid the
 * precision issue. They have to stop
 * computing digits once they get past the maximum length of the significand.
 */
class ExactFloat(
    val sign: Boolean,
    val exponent: Int,
    val significand: BigInteger
) : Comparable<ExactFloat> {

    constructor(bigInt: BigInteger) : this(bigInt < BigInteger.ZERO, 0, bigInt.abs())

    operator fun plus(other: ExactFloat): ExactFloat {
        val expoDiff = this.exponent - other.exponent
        val finalExpo = min(this.exponent.toDouble(), other.exponent.toDouble()).toInt()

        if (this.sign != other.sign) {
            val comp = this.abs().compareTo(other.abs())
            // Section 6.3
            if (comp == 0) {
                // Caller should handle what to do in the event a zero pops out
                return ExactFloat(false, 0, BigInteger.ZERO)
            }

            val finalSign = if (comp > 0) this.sign else other.sign
            return if (expoDiff < 0) {
                ExactFloat(
                    finalSign, finalExpo,
                    this.significand.subtract(other.significand.shiftLeft(-expoDiff)).abs()
                )
            } else {
                ExactFloat(
                    finalSign, finalExpo,
                    this.significand.shiftLeft(expoDiff).subtract(other.significand).abs()
                )
            }
        } else {
            return if (expoDiff < 0) {
                ExactFloat(
                    this.sign,
                    finalExpo,
                    this.significand.add(other.significand.shiftLeft(-expoDiff)).abs()
                )
            } else {
                ExactFloat(
                    this.sign,
                    finalExpo,
                    this.significand.shiftLeft(expoDiff).add(other.significand).abs()
                )
            }
        }
    }

    operator fun times(other: ExactFloat): ExactFloat {
        // 0 * x = 0
        // Sign is the xor of the input signs - Section 6.3
        if (this.isZero || other.isZero) {
            return ExactFloat(this.sign != other.sign, 0, BigInteger.ZERO)
        }
        return ExactFloat(
            this.sign != other.sign,
            this.exponent + other.exponent,
            this.significand.multiply(other.significand)
        )
    }

    /**
     * Divides this number by another
     *
     *
     * This method uses simple long division rather than a more sophisticated
     * process to compute a/b. This is mainly
     * because it needs to be completely correct for a specific amount of bytes to
     * be able to round correctly, and
     * because exact divisions such as 1/2 will be completely exact. This is not a
     * guarantee with many other methods.
     *
     *
     * Many implementation use Newton–Raphson division because it is much faster,
     * but the analysis to guarantee
     * correct rounding behavior is beyond me.
     *
     * @param other
     * the dividend
     * @param accuracy
     * the number of bits to compute
     * @return An exact float that equals this/other with the first accuracy bits
     * correct
     * @author Benjamin Landers
     */
    fun divide(other: ExactFloat, accuracy: Int): ExactFloat {
        assert(accuracy > 0) { "Accuracy must be a positive number" }
        assert(!other.isZero) { "Divide by Zero is not valid" }
        val a = this.normalize()
        val b = other.normalize()
        var divisor = a.significand
        var dividend = b.significand
        val expChange = dividend.bitLength() - divisor.bitLength()
        // Line up the numbers
        if (divisor.bitLength() > dividend.bitLength()) {
            dividend = dividend.shiftLeft(divisor.bitLength() - dividend.bitLength())
        } else {
            divisor = divisor.shiftLeft(dividend.bitLength() - divisor.bitLength())
        }

        var count = 0
        // Perform long division
        var outbits = BigInteger.ZERO
        while (outbits.bitLength() < accuracy) {
            outbits = outbits.shiftLeft(1)
            if (divisor >= dividend) {
                divisor = divisor.subtract(dividend)
                outbits = outbits.add(BigInteger.ONE)
            }
            count++
            divisor = divisor.shiftLeft(1)
            if (divisor == BigInteger.ZERO) {
                break
            }
        }

        // Always round up so exact results are distinguished from rounded ones.
        if (outbits.bitLength() >= accuracy) {
            outbits = outbits.shiftLeft(1)
            outbits = outbits.add(BigInteger.ONE)
            count++
        }
        return ExactFloat(a.sign != b.sign, a.exponent - b.exponent - count - expChange + 1, outbits)
    }


    fun squareRoot(accuracy: Int): ExactFloat {
        assert(!this.sign) { "Square root of a negative number is not real" }
        val normed = this.normalize()
        // Start with a first guess relatively close to (but definitely above) the
        // actual square root.
        // And shrink it until it is below
        var exp = (normed.exponent + normed.significand.bitLength()) / 2 + 1
        while (true) {
            val tmp = ExactFloat(false, exp, BigInteger.ONE)
            val cmp = normed.compareTo(tmp.times(tmp))
            if (cmp == 0) {
                return tmp
            } else if (cmp > 0) {
                break
            }
            exp--
        }

        // Then use binary search until accuracy bits are determined
        var a = ExactFloat(false, exp, BigInteger.ONE)
        var b = ExactFloat(false, exp + 1, BigInteger.ONE)
        repeat(accuracy) {
            val tmp = a.plus(b).shiftRight(1)
            val cmp = normed.compareTo(tmp * tmp)
            if (cmp == 0) {
                return tmp
            } else if (cmp > 0) {
                a = tmp
            } else {
                b = tmp
            }
        }
        val tmp = a.plus(b).shiftRight(1)
        val cmp = normed.compareTo(tmp * tmp)
        return if (cmp == 0) {
            tmp
        } else if (cmp > 0) {
            tmp.plus(b).shiftRight(1) // ensure it doesn't round incorrectly
        } else {
            tmp.plus(a).shiftRight(1) // ensure it doesn't round incorrectly
        }
    }

    fun shiftRight(i: Int) = ExactFloat(this.sign, this.exponent - i, this.significand)

    fun normalize(): ExactFloat = if (this.isZero) {
        ExactFloat(this.sign, 0, BigInteger.ZERO)
    } else ExactFloat(
        this.sign,
        this.exponent + this.significand.lowestSetBit,
        this.significand.shiftRight(this.significand.lowestSetBit)
    )

    fun roundToIntegral(env: Environment): ExactFloat {
        if (this.isZero) {
            return this
        }
        val f = this.normalize()
        if (f.exponent >= 0) {
            return f
        }

        val bitsToRound = -f.exponent
        val mainBits = f.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound)

        val zeroRounded = ExactFloat(f.sign, f.exponent, mainBits).normalize()
        val oneRounded = zeroRounded.plus(ExactFloat(f.sign, 0, BigInteger.valueOf(1)))
        if (env.mode == RoundingMode.ZERO) {
            return zeroRounded
        }

        if (env.mode == RoundingMode.MAX || env.mode == RoundingMode.MIN) {
            return if ((env.mode == RoundingMode.MAX) == f.sign) {
                // if we are rounding towards zero (max & < 0 or min & > 0)
                zeroRounded
            } else {
                // Alternatively away from zero (round to zero + 1)
                oneRounded
            }
        }

        val roundedBits = f.significand.subtract(mainBits)
        return if (roundedBits == BigInteger.ONE.shiftLeft(bitsToRound - 1)) {
            // If there is a tie round according to the rounding mode
            if (env.mode == RoundingMode.AWAY || zeroRounded.significand.testBit(0)) {
                oneRounded
            } else {
                zeroRounded
            }
        } else if (roundedBits > BigInteger.ONE.shiftLeft(bitsToRound - 1)) { // TODO: check sign
            oneRounded
        } else {
            zeroRounded
        }
    }

    fun toBigInteger(env: Environment): BigInteger {
        if (this.isZero) return BigInteger.ZERO

        val f = this.roundToIntegral(env).normalize()
        if (f.compareTo(this) != 0) {
            env.inexact = true
        }
        assert(f.exponent >= 0) { "There can't be any fractions at this point" }
        return f.significand.shiftLeft(f.exponent).multiply(BigInteger.valueOf((if (this.sign) -1 else 1).toLong()))
    }

    override fun compareTo(other: ExactFloat): Int {
        if (this.isZero) {
            if (other.isZero) {
                return 0
            }
            return if (other.sign) 1 else -1
        }
        if (this.sign != other.sign) {
            return if (this.sign) -1 else 1
        }
        val tmp = this.exponent - other.exponent + this.significand.bitLength() - other.significand.bitLength()
        return if (tmp > 0) {
            if (this.sign) -1 else 1
        } else if (tmp < 0) {
            if (this.sign) 1 else -1
        } else {
            if (this.exponent < other.exponent) {
                this.significand.compareTo(other.significand.shiftLeft(other.exponent - this.exponent)) * (if (this.sign) -1 else 1)
            } else {
                this.significand.shiftLeft(this.exponent - other.exponent)
                    .compareTo(other.significand) * (if (this.sign) -1 else 1)
            }
        }
    }

    fun abs(): ExactFloat = ExactFloat(false, this.exponent, this.significand)
    fun negate(): ExactFloat = ExactFloat(!this.sign, this.exponent, this.significand)
    val isZero: Boolean get() = this.significand == BigInteger.ZERO
}