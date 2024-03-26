package io.github.chr1sps.jsoftfloat.types;

import java.math.BigInteger;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.Flags;
import io.github.chr1sps.jsoftfloat.RoundingMode;
import io.github.chr1sps.jsoftfloat.internal.ExactFloat;

/**
 * Represents the Binary32 format
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class Float64 extends Floating<Float64> {
    // TODO: make a more abstract binary float class
    /** Constant <code>Zero</code> */
    /** Constant <code>NegativeZero</code> */
    /** Constant <code>NaN</code> */
    /** Constant <code>Infinity</code> */
    /**
     * Constant <code>NegativeInfinity</code>
     */
    public static final Float64 Zero = new Float64(0),
            NegativeZero = new Float64(0x80000000_00000000L),
            NaN = new Float64(0x7FF80000_00000000L),
            Infinity = new Float64(0x7FF00000_00000000L),
            NegativeInfinity = new Float64(0xFFF00000_00000000L);

    public final long bits;

    /**
     * <p>Constructor for Float64.</p>
     *
     * @param bits a long
     */
    public Float64(long bits) {
        this.bits = bits;
    }

    /**
     * <p>Constructor for Float64.</p>
     *
     * @param sign        a boolean
     * @param exponent    a int
     * @param significand a long
     */
    public Float64(boolean sign, int exponent, long significand) {
        this(((sign) ? 0x80000000_00000000L : 0) | (((exponent + 1023) & 0x7FFL) << sigbits) | (significand & sigmask));
    }

    /**
     * <p>exponent.</p>
     *
     * @return a int
     */
    public int exponent() {
        return ((int) (bits >>> sigbits) & 0x7FF) - 1023;
    }

    /**
     * <p>fromInteger.</p>
     *
     * @param num An integer to be converted to
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public static Float64 fromInteger(long num) {
        if (num == 0)
            return Zero;
        boolean sign = num < 0;
        num = sign ? -num : num;
        int exponent = 0;
        long significand = 0;
        for (int i = 62; i >= 0; i--) {
            if (((num >> i) & 1) == 1) {
                exponent = i;
                significand = (num << (64 - i)) >>> 11;
                break;
            }
        }
        return new Float64(sign, exponent, significand);
    }

    /**
     * <p>negate.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public Float64 negate() {
        return new Float64(bits ^ 0x80000000_00000000L); // Flip the sign bit
    }

    /**
     * <p>abs.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public Float64 abs() {
        return new Float64(bits & 0x7FFFFFFF_FFFFFFFFL);
    }

    /**
     * <p>copySign.</p>
     *
     * @param signToTake a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public Float64 copySign(Float64 signToTake) {
        return new Float64((bits & 0x7FFFFFFF_FFFFFFFFL) | (signToTake.bits & 0x80000000_00000000L));
    }

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    public boolean isSignMinus() {
        return (bits >>> 63) == 1;
    }

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    public boolean isInfinite() {
        return exponent() == 1024 && (bits & sigmask) == 0;
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    public boolean isNormal() {
        return exponent() != -1023 && exponent() != 1024;
    }

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    public boolean isSubnormal() {
        return exponent() == -1023 && !isZero();
    }

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    public boolean isNaN() {
        return exponent() == 1024 && !isInfinite();
    }

    /**
     * <p>isSignalling.</p>
     *
     * @return a boolean
     */
    public boolean isSignalling() {
        if (!isNaN())
            return false;
        return (bits & 0x80000_00000000L) == 0;
    }

    /**
     * <p>isCanonical.</p>
     *
     * @return a boolean
     */
    public boolean isCanonical() {
        // TODO: implement
        return true;
    }

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    public boolean isZero() {
        return bits == 0 || bits == 0x80000000_00000000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 NaN() {
        return NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 Zero() {
        return Zero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 NegativeZero() {
        return NegativeZero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 Infinity() {
        return Infinity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 NegativeInfinity() {
        return NegativeInfinity;
    }

    // Some constants that allow fromExactFloat to be mostly copied
    private static final int sigbits = 52, expbits = 11,
            maxexp = 1 << (expbits - 1),
            minexp = -(1 << (expbits - 1)) + 1;
    private static final long sigmask = (1L << sigbits) - 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public Float64 fromExactFloat(ExactFloat ef, Environment env) {
        if (ef.isZero()) {
            return ef.sign ? Float64.NegativeZero : Float64.Zero;
        }
        ef = ef.normalize();
        int normalizedExponent = ef.exponent + ef.significand.bitLength();

        // Used to calculate how to round at the end
        Float64 awayZero, towardsZero;
        BigInteger roundedBits;
        int bitsToRound;

        if (normalizedExponent <= minexp + 1) {
            // Subnormal

            if (ef.exponent > minexp - sigbits) {
                assert ef.significand.bitLength() <= sigbits : "Its actually normal";
                return new Float64(ef.sign, minexp,
                        ef.significand.shiftLeft(-(minexp - sigbits + 1) + ef.exponent).longValueExact());
            }

            env.flags.add(Flags.inexact);
            env.flags.add(Flags.underflow); // Section 7.5
            bitsToRound = (minexp - sigbits + 1) - ef.exponent;
            BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            towardsZero = new Float64(ef.sign, minexp, ef.significand.shiftRight(bitsToRound).longValueExact());
            BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));
            if (upBits.testBit(0) || upBits.bitLength() <= sigbits) {
                assert upBits.bitLength() <= sigbits;
                awayZero = new Float64(ef.sign, minexp, upBits.longValueExact());
            } else {
                awayZero = new Float64(ef.sign, minexp + 1, upBits.longValueExact() & sigmask);
            }
        } else if (normalizedExponent > maxexp) {
            // Section 7.4
            env.flags.add(Flags.overflow);
            env.flags.add(Flags.inexact);
            switch (env.mode) {
                case zero:
                    return new Float64(ef.sign, maxexp - 1, -1); // Largest finite number
                case min:
                case max:
                    if (ef.sign != (env.mode == RoundingMode.max)) {
                        return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
                    } else {
                        return new Float64(ef.sign, maxexp - 1, -1); // Largest finite number
                    }
                case away:
                case even:
                    return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
            }
            assert false : "Not reachable";
            return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
        } else {
            if (ef.significand.bitLength() <= (sigbits + 1)) {
                // No rounding needed
                assert ef.exponent + ef.significand.bitLength() - 1 > minexp : "Its actually subnormal";
                Float64 a = new Float64(ef.sign, ef.exponent + ef.significand.bitLength() - 1,
                        ef.significand.shiftLeft((sigbits + 1) - ef.significand.bitLength()).longValueExact()
                                & sigmask);

                return a;
            }
            env.flags.add(Flags.inexact);
            bitsToRound = ef.significand.bitLength() - (sigbits + 1);
            BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));

            towardsZero = new Float64(ef.sign, ef.exponent + sigbits + bitsToRound,
                    ef.significand.shiftRight(bitsToRound).longValueExact() & sigmask);
            if (upBits.testBit(0) || upBits.bitLength() <= sigbits + 1) {
                awayZero = new Float64(ef.sign, ef.exponent + sigbits + bitsToRound, upBits.longValueExact() & sigmask);
            } else {
                awayZero = new Float64(ef.sign, ef.exponent + (sigbits + 1) + bitsToRound,
                        upBits.shiftRight(1).longValueExact() & sigmask);
            }

        }

        // Either round towards or away from zero based on rounding mode
        switch (env.mode) {
            case zero:
                return towardsZero;
            case max:
            case min:
                if (ef.sign != (env.mode == RoundingMode.max)) {
                    return awayZero;
                } else {
                    return towardsZero;
                }
            default:
                break;
        }

        // See which result is closer to the non-rounded version
        if (roundedBits.equals(BigInteger.ONE.shiftLeft(bitsToRound - 1))) {
            if (env.mode == RoundingMode.away || (awayZero.bits & 1) == 0) {
                return awayZero;
            } else {
                return towardsZero;
            }
        } else if (roundedBits.compareTo(BigInteger.ONE.shiftLeft(bitsToRound - 1)) > 0) {
            return awayZero;
        } else {
            return towardsZero;
        }
    }

    /**
     * <p>fromExact.</p>
     *
     * @param ef a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     * @param e  a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
     */
    public static Float64 fromExact(ExactFloat ef, Environment e) {
        return Zero.fromExactFloat(ef, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExactFloat toExactFloat() {
        assert !isInfinite() : "Infinity is not exact";
        assert !isNaN() : "NaNs are not exact";
        assert !isZero() : "Zeros should be handled explicitly";

        boolean sign = isSignMinus();
        int exponent;
        BigInteger significand;
        if (isZero()) {
            exponent = 0;
            significand = BigInteger.ZERO;
        } else if (isNormal()) {
            exponent = exponent() - sigbits;
            significand = BigInteger.valueOf((bits & sigmask) + (sigmask + 1)); // Add back the implied one
        } else if (isSubnormal()) {
            exponent = exponent() - (sigbits - 1);
            significand = BigInteger.valueOf(bits & sigmask);
        } else {
            assert false : "This should not be reachable";
            return null;
        }
        return new ExactFloat(sign, exponent, significand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int maxPrecision() {
        // TODO: make a tight bound around actual required precision
        return 60;
    }
}
