package rars.jsoftfloat.types;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.jsoftfloat.internal.ExactFloat;

import java.math.BigInteger;

/**
 * Represents the Binary32 format
 */
public final class Float64 implements Floating<Float64> {
    // TODO: make a more abstract binary float class
    /**
     * Constant {@code Zero}
     */
    public static final Float64 Zero = new Float64(0);
    /**
     * Constant {@code NegativeZero}
     */
    public static final Float64 NegativeZero = new Float64(0x80000000_00000000L);
    /**
     * Constant {@code NaN}
     */
    public static final Float64 NaN = new Float64(0x7FF80000_00000000L);
    /**
     * Constant {@code Infinity}
     */
    public static final Float64 Infinity = new Float64(0x7FF00000_00000000L);
    /**
     * Constant {@code NegativeInfinity}
     */
    public static final Float64 NegativeInfinity = new Float64(0xFFF00000_00000000L);
    // Some constants that allow fromExactFloat to be mostly copied
    private static final int sigbits = 52, expbits = 11,
        maxexp = 1 << (Float64.expbits - 1),
        minexp = -(1 << (Float64.expbits - 1)) + 1;
    private static final long sigmask = (1L << Float64.sigbits) - 1;
    public final long bits;

    /**
     * <p>Constructor for Float64.</p>
     *
     * @param bits
     *     a long
     */
    public Float64(final long bits) {
        this.bits = bits;
    }

    /**
     * <p>Constructor for Float64.</p>
     *
     * @param sign
     *     a boolean
     * @param exponent
     *     a int
     * @param significand
     *     a long
     */
    public Float64(final boolean sign, final int exponent, final long significand) {
        this((
            (sign)
                ? 0x80000000_00000000L
                : 0
        ) | (((exponent + 1023) & 0x7FFL) << Float64.sigbits) | (significand & Float64.sigmask));
    }

//    /**
//     * <p>fromInteger.</p>
//     *
//     * @param num An integer to be converted to
//     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
//     */
//    public static Float64 fromInteger(long num) {
//        if (num == 0)
//            return Float64.Zero;
//        final boolean sign = num < 0;
//        num = sign ? -num : num;
//        int exponent = 0;
//        long significand = 0;
//        for (int i = 62; i >= 0; i--) {
//            if (((num >> i) & 1) == 1) {
//                exponent = i;
//                significand = (num << (64 - i)) >>> 11;
//                break;
//            }
//        }
//        return new Float64(sign, exponent, significand);
//    }
//
//    /**
//     * <p>fromExact.</p>
//     *
//     * @param ef a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
//     * @param e  a {@link io.github.chr1sps.jsoftfloat.Environment} object
//     * @return a {@link io.github.chr1sps.jsoftfloat.types.Float64} object
//     */
//    public static Float64 fromExact(@NotNull final ExactFloat ef, @NotNull final Environment e) {
//        return Float64.Zero.fromExactFloat(ef, e);
//    }

    /**
     * <p>exponent.</p>
     *
     * @return a int
     */
    public int exponent() {
        return ((int) (this.bits >>> Float64.sigbits) & 0x7FF) - 1023;
    }

    /**
     * <p>negate.</p>
     *
     * @return a {@link Float64} object
     */
    @Override
    public @NotNull Float64 negate() {
        return new Float64(this.bits ^ 0x80000000_00000000L); // Flip the sign bit
    }

    /**
     * <p>abs.</p>
     *
     * @return a {@link Float64} object
     */
    public @NotNull Float64 abs() {
        return new Float64(this.bits & 0x7FFFFFFF_FFFFFFFFL);
    }

    /**
     * <p>copySign.</p>
     *
     * @param signToTake
     *     a {@link Float64} object
     * @return a {@link Float64} object
     */
    public @NotNull Float64 copySign(@NotNull final Float64 signToTake) {
        return new Float64((this.bits & 0x7FFFFFFF_FFFFFFFFL) | (signToTake.bits & 0x80000000_00000000L));
    }

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isSignMinus() {
        return (this.bits >>> 63) == 1;
    }

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isInfinite() {
        return this.exponent() == 1024 && (this.bits & Float64.sigmask) == 0;
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isNormal() {
        return this.exponent() != -1023 && this.exponent() != 1024;
    }

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isSubnormal() {
        return this.exponent() == -1023 && !this.isZero();
    }

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isNaN() {
        return this.exponent() == 1024 && !this.isInfinite();
    }

    /**
     * <p>isSignalling.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isSignalling() {
        if (!this.isNaN()) {
            return false;
        }
        return (this.bits & 0x80000_00000000L) == 0;
    }

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isZero() {
        return this.bits == 0 || this.bits == 0x80000000_00000000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 NaN() {
        return Float64.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 Zero() {
        return Float64.Zero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 NegativeZero() {
        return Float64.NegativeZero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 Infinity() {
        return Float64.Infinity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 NegativeInfinity() {
        return Float64.NegativeInfinity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float64 fromExactFloat(@NotNull ExactFloat ef, @NotNull final Environment env) {
        if (ef.isZero()) {
            return ef.sign ? Float64.NegativeZero : Float64.Zero;
        }
        ef = ef.normalize();
        final int normalizedExponent = ef.exponent + ef.significand.bitLength();

        // Used to calculate how to round at the end
        final Float64 awayZero;
        final Float64 towardsZero;
        final BigInteger roundedBits;
        final int bitsToRound;

        if (normalizedExponent <= Float64.minexp + 1) {
            // Subnormal

            if (ef.exponent > Float64.minexp - Float64.sigbits) {
                assert ef.significand.bitLength() <= Float64.sigbits : "Its actually normal";
                return new Float64(
                    ef.sign, Float64.minexp,
                    ef.significand.shiftLeft(-(Float64.minexp - Float64.sigbits + 1) + ef.exponent).longValueExact()
                );
            }

            env.inexact = true;
            env.underflow = true; // Section 7.5
            bitsToRound = (Float64.minexp - Float64.sigbits + 1) - ef.exponent;
            final BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            towardsZero = new Float64(ef.sign, Float64.minexp, ef.significand.shiftRight(bitsToRound).longValueExact());
            final BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));
            if (upBits.testBit(0) || upBits.bitLength() <= Float64.sigbits) {
                assert upBits.bitLength() <= Float64.sigbits;
                awayZero = new Float64(ef.sign, Float64.minexp, upBits.longValueExact());
            } else {
                awayZero = new Float64(ef.sign, Float64.minexp + 1, upBits.longValueExact() & Float64.sigmask);
            }
        } else if (normalizedExponent > Float64.maxexp) {
            // Section 7.4
            env.overflow = true;
            env.inexact = true;
            switch (env.mode) {
                case ZERO:
                    return new Float64(ef.sign, Float64.maxexp - 1, -1); // Largest finite number
                case MIN:
                case MAX:
                    if (ef.sign != (env.mode == RoundingMode.MAX)) {
                        return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
                    } else {
                        return new Float64(ef.sign, Float64.maxexp - 1, -1); // Largest finite number
                    }
                case AWAY:
                case EVEN:
                    return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
            }
            assert false : "Not reachable";
            return ef.sign ? Float64.NegativeInfinity : Float64.Infinity;
        } else {
            if (ef.significand.bitLength() <= (Float64.sigbits + 1)) {
                // No rounding needed
                final int tmp = ef.exponent + ef.significand.bitLength() - 1;
                assert tmp > Float64.minexp : "Its actually subnormal";

                return new Float64(
                    ef.sign, tmp,
                    ef.significand.shiftLeft((Float64.sigbits + 1) - ef.significand.bitLength()).longValueExact()
                        & Float64.sigmask
                );
            }
            env.inexact = true;
            bitsToRound = ef.significand.bitLength() - (Float64.sigbits + 1);
            final BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            final BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));

            towardsZero = new Float64(
                ef.sign, ef.exponent + Float64.sigbits + bitsToRound,
                ef.significand.shiftRight(bitsToRound).longValueExact() & Float64.sigmask
            );
            if (upBits.testBit(0) || upBits.bitLength() <= Float64.sigbits + 1) {
                awayZero = new Float64(
                    ef.sign,
                    ef.exponent + Float64.sigbits + bitsToRound,
                    upBits.longValueExact() & Float64.sigmask
                );
            } else {
                awayZero = new Float64(
                    ef.sign, ef.exponent + (Float64.sigbits + 1) + bitsToRound,
                    upBits.shiftRight(1).longValueExact() & Float64.sigmask
                );
            }

        }

        // Either round towards or away from zero based on rounding mode
        switch (env.mode) {
            case ZERO:
                return towardsZero;
            case MAX:
            case MIN:
                if (ef.sign != (env.mode == RoundingMode.MAX)) {
                    return awayZero;
                } else {
                    return towardsZero;
                }
            default:
                break;
        }

        // See which result is closer to the non-rounded version
        if (roundedBits.equals(BigInteger.ONE.shiftLeft(bitsToRound - 1))) {
            if (env.mode == RoundingMode.AWAY || (awayZero.bits & 1) == 0) {
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
     * {@inheritDoc}
     */
    @Override
    public @NotNull ExactFloat toExactFloat() {
        assert !this.isInfinite() : "Infinity is not exact";
        assert !this.isNaN() : "NaNs are not exact";
        assert !this.isZero() : "Zeros should be handled explicitly";

        final boolean sign = this.isSignMinus();
        final int exponent;
        final BigInteger significand;
        if (this.isZero()) {
            exponent = 0;
            significand = BigInteger.ZERO;
        } else if (this.isNormal()) {
            exponent = this.exponent() - Float64.sigbits;
            significand = BigInteger.valueOf((this.bits & Float64.sigmask) + (Float64.sigmask + 1)); // Add back the implied one
        } else if (this.isSubnormal()) {
            exponent = this.exponent() - (Float64.sigbits - 1);
            significand = BigInteger.valueOf(this.bits & Float64.sigmask);
        } else {
            assert false : "This should not be reachable";
            // Dummy value
            return new ExactFloat(sign, 0, BigInteger.ZERO);
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
