package io.github.chr1sps.jsoftfloat.internal;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.RoundingMode;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * A helper type to generalize floating point exact operations. This helps to
 * reduce the extra rounding code into just
 * one place. It isn't able to refer to every exact float though. 1/3 is not
 * representable as it requires infinite
 * precision, but any finite significand is representable.
 * <p>
 * Square Root and Division cannot really use this because they cannot avoid the
 * precision issue. They have to stop
 * computing digits once they get past the maximum length of the significand.
 */
public class ExactFloat implements Comparable<ExactFloat> {
    // Value = (-1)^sign * significand * 2^exponent
    public final boolean sign;
    public final int exponent;
    public final @NotNull BigInteger significand;

    /**
     * <p>Constructor for ExactFloat.</p>
     *
     * @param sign a boolean
     * @param exp  a int
     * @param sig  a {@link java.math.BigInteger} object
     */
    public ExactFloat(final boolean sign, final int exp, @NotNull final BigInteger sig) {
        this.sign = sign;
        this.exponent = exp;
        this.significand = sig;
    }

    /**
     * <p>Constructor for ExactFloat.</p>
     *
     * @param i a {@link java.math.BigInteger} object
     */
    public ExactFloat(@NotNull BigInteger i) {
        if (i.compareTo(BigInteger.ZERO) < 0) {
            this.sign = true;
            i = i.negate();
        } else {
            this.sign = false;
        }
        this.exponent = 0;
        this.significand = i;
    }

    /**
     * <p>add.</p>
     *
     * @param other a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat add(@NotNull final ExactFloat other) {
        final int expoDiff = this.exponent - other.exponent;
        final int finalExpo = Math.min(this.exponent, other.exponent);

        if (this.sign != other.sign) {
            final int comp = this.abs().compareTo(other.abs());
            // Section 6.3
            if (comp == 0) {
                // Caller should handle what to do in the event a zero pops out
                return new ExactFloat(false, 0, BigInteger.ZERO);
            }

            final boolean finalSign = (comp > 0) ? this.sign : other.sign;
            if (expoDiff < 0) {
                return new ExactFloat(finalSign, finalExpo,
                        this.significand.subtract(other.significand.shiftLeft(-expoDiff)).abs());
            } else {
                return new ExactFloat(finalSign, finalExpo,
                        this.significand.shiftLeft(expoDiff).subtract(other.significand).abs());
            }
        } else {
            if (expoDiff < 0) {
                return new ExactFloat(this.sign, finalExpo, this.significand.add(other.significand.shiftLeft(-expoDiff)).abs());
            } else {
                return new ExactFloat(this.sign, finalExpo, this.significand.shiftLeft(expoDiff).add(other.significand).abs());
            }
        }
    }

    /**
     * <p>multiply.</p>
     *
     * @param other a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat multiply(@NotNull final ExactFloat other) {
        // 0 * x = 0
        // Sign is the xor of the input signs - Section 6.3
        if (this.isZero() || other.isZero()) {
            return new ExactFloat(this.sign != other.sign, 0, BigInteger.ZERO);
        }
        return new ExactFloat(this.sign != other.sign, this.exponent + other.exponent, this.significand.multiply(other.significand));
    }

    /**
     * Divides this number by another
     * <p>
     * This method uses simple long division rather than a more sophisticated
     * process to compute a/b. This is mainly
     * because it needs to be completely correct for a specific amount of bytes to
     * be able to round correctly, and
     * because exact divisions such as 1/2 will be completely exact. This is not a
     * guarantee with many other methods.
     * <p>
     * Many implementation use Newton–Raphson division because it is much faster,
     * but the analysis to guarantee
     * correct rounding behavior is beyond me.
     *
     * @param other    the dividend
     * @param accuracy the number of bits to compute
     * @return An exact float that equals this/other with the first accuracy bits
     * correct
     * @author Benjamin Landers
     */
    public @NotNull ExactFloat divide(@NotNull final ExactFloat other, final int accuracy) {
        assert accuracy > 0 : "Accuracy must be a positive number";
        assert !other.isZero() : "Divide by Zero is not valid";
        final ExactFloat a = this.normalize();
        final ExactFloat b = other.normalize();
        BigInteger divisor = a.significand, dividend = b.significand;
        BigInteger outbits = BigInteger.ZERO;
        final int expChange = dividend.bitLength() - divisor.bitLength();
        // Line up the numbers
        if (divisor.bitLength() > dividend.bitLength()) {
            dividend = dividend.shiftLeft(divisor.bitLength() - dividend.bitLength());
        } else {
            divisor = divisor.shiftLeft(dividend.bitLength() - divisor.bitLength());
        }

        int count = 0;
        // Perform long division
        while (outbits.bitLength() < accuracy) {
            outbits = outbits.shiftLeft(1);
            if (divisor.compareTo(dividend) >= 0) {
                divisor = divisor.subtract(dividend);
                outbits = outbits.add(BigInteger.ONE);

            }
            count++;
            divisor = divisor.shiftLeft(1);
            if (divisor.equals(BigInteger.ZERO)) {
                break;
            }
        }

        // Always round up so exact results are distinguished from rounded ones.
        if (outbits.bitLength() >= accuracy) {
            outbits = outbits.shiftLeft(1);
            outbits = outbits.add(BigInteger.ONE);
            count++;
        }
        return new ExactFloat(a.sign != b.sign, a.exponent - b.exponent - count - expChange + 1, outbits);
    }

    /**
     * <p>squareRoot.</p>
     *
     * @param accuracy a int
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat squareRoot(final int accuracy) {
        assert !this.sign : "Square root of a negative number is not real";
        final ExactFloat normed = this.normalize();
        // Start with a first guess relatively close to (but definitely above) the
        // actual square root.
        // And shrink it until it is below
        int exp = (normed.exponent + normed.significand.bitLength()) / 2 + 1;
        while (true) {
            final ExactFloat tmp = new ExactFloat(false, exp, BigInteger.ONE);
            final int cmp = normed.compareTo(tmp.multiply(tmp));
            if (cmp == 0) {
                return tmp;
            } else if (cmp > 0) {
                break;
            }
            exp--;
        }

        // Then use binary search until accuracy bits are determined
        ExactFloat a = new ExactFloat(false, exp, BigInteger.ONE), b = new ExactFloat(false, exp + 1, BigInteger.ONE);
        for (int i = 0; i < accuracy; i++) {
            final ExactFloat tmp = a.add(b).shiftRight(1);
            final int cmp = normed.compareTo(tmp.multiply(tmp));
            if (cmp == 0) {
                return tmp;
            } else if (cmp > 0) {
                a = tmp;
            } else {
                b = tmp;
            }
        }
        final ExactFloat tmp = a.add(b).shiftRight(1);
        final int cmp = normed.compareTo(tmp.multiply(tmp));
        if (cmp == 0) {
            return tmp;
        } else if (cmp > 0) {
            return tmp.add(b).shiftRight(1); // ensure it doesn't round incorrectly
        } else {
            return tmp.add(a).shiftRight(1); // ensure it doesn't round incorrectly
        }
    }

    /**
     * <p>shiftRight.</p>
     *
     * @param i a int
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat shiftRight(final int i) {
        return new ExactFloat(this.sign, this.exponent - i, this.significand);
    }

    /**
     * <p>normalize.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat normalize() {
        if (this.isZero())
            return new ExactFloat(this.sign, 0, BigInteger.ZERO);
        return new ExactFloat(this.sign, this.exponent + this.significand.getLowestSetBit(),
                this.significand.shiftRight(this.significand.getLowestSetBit()));
    }

    /**
     * <p>roundToIntegral.</p>
     *
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat roundToIntegral(@NotNull final Environment env) {
        if (this.isZero())
            return this;
        final ExactFloat f = this.normalize();
        if (f.exponent >= 0)
            return f;

        final int bitsToRound = -f.exponent;
        final BigInteger mainBits = f.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
        final BigInteger roundedBits = f.significand.subtract(mainBits);

        final ExactFloat zeroRounded = new ExactFloat(f.sign, f.exponent, mainBits).normalize();
        final ExactFloat oneRounded = zeroRounded.add(new ExactFloat(f.sign, 0, BigInteger.valueOf(1)));
        if (env.mode == RoundingMode.zero) {
            return zeroRounded;
        }

        if (env.mode == RoundingMode.max || env.mode == RoundingMode.min) {
            if ((env.mode == RoundingMode.max) == f.sign) {
                // if we are rounding towards zero (max & < 0 or min & > 0)
                return zeroRounded;
            } else {
                // Alternatively away from zero (round to zero + 1)
                return oneRounded;
            }
        }

        if (roundedBits.equals(BigInteger.ONE.shiftLeft(bitsToRound - 1))) {
            // If there is a tie round according to the rounding mode
            if (env.mode == RoundingMode.away || zeroRounded.significand.testBit(0)) {
                return oneRounded;
            } else {
                return zeroRounded;
            }
        } else if (roundedBits.compareTo(BigInteger.ONE.shiftLeft(bitsToRound - 1)) > 0) { // TODO: check sign
            return oneRounded;
        } else {
            return zeroRounded;
        }
    }

    /**
     * <p>toIntegral.</p>
     *
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a {@link java.math.BigInteger} object
     */
    public @NotNull BigInteger toIntegral(@NotNull final Environment env) {
        if (this.isZero())
            return BigInteger.ZERO;

        final ExactFloat f = this.roundToIntegral(env).normalize();
        if (f.compareTo(this) != 0) {
            env.inexact = true;
        }
        assert f.exponent >= 0 : "There can't be any fractions at this point";
        return f.significand.shiftLeft(f.exponent).multiply(BigInteger.valueOf(this.sign ? -1 : 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NotNull final ExactFloat other) {
        if (this.isZero()) {
            if (other.isZero()) {
                return 0;
            }
            return other.sign ? 1 : -1;
        }
        if (this.sign != other.sign) {
            return this.sign ? -1 : 1;
        }
        final var tmp = this.exponent - other.exponent + this.significand.bitLength() - other.significand.bitLength();
        if (tmp > 0) {
            return this.sign ? -1 : 1;
        } else if (tmp < 0) {
            return this.sign ? 1 : -1;
        } else {
            if (this.exponent < other.exponent) {
                return this.significand.compareTo(other.significand.shiftLeft(other.exponent - this.exponent)) * (this.sign ? -1 : 1);
            } else {
                return this.significand.shiftLeft(this.exponent - other.exponent).compareTo(other.significand) * (this.sign ? -1 : 1);
            }
        }
    }

    /**
     * <p>abs.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat abs() {
        return new ExactFloat(false, this.exponent, this.significand);
    }

    /**
     * <p>negate.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public @NotNull ExactFloat negate() {
        return new ExactFloat(!this.sign, this.exponent, this.significand);
    }

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    public boolean isZero() {
        return this.significand.equals(BigInteger.ZERO);
    }
}
