package rars.jsoftfloat.types;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.jsoftfloat.internal.ExactFloat;

import java.math.BigInteger;

/**
 * Represents the Binary32 format
 */
public final class Float32 implements Floating<Float32> {
    // TODO: make a more abstract binary float class
    /**
     * Constant {@code Zero}
     */
    public static final Float32 Zero = new Float32(0),
    /**
     * Constant {@code NegativeZero}
     */
    NegativeZero = new Float32(0x80000000),
    /**
     * Constant {@code NaN}
     */
    NaN = new Float32(0x7FC00000),
    /**
     * Constant {@code Infinity}
     */
    Infinity = new Float32(0x7F800000),
    /**
     * Constant {@code NegativeInfinity}
     */
    NegativeInfinity = new Float32(0xFF800000);

    public final int bits;

    /**
     * <p>Constructor for Float32.</p>
     *
     * @param bits
     *     a int
     */
    public Float32(final int bits) {
        this.bits = bits;
    }

    /**
     * <p>Constructor for Float32.</p>
     *
     * @param sign
     *     a boolean
     * @param exponent
     *     a int
     * @param significand
     *     a int
     */
    public Float32(final boolean sign, final int exponent, final int significand) {
        this(((sign) ? 0x80000000 : 0) | (((exponent + 127) & 0xFF) << 23) | (significand & 0x007FFFFF));
    }

    /**
     * <p>fromInteger.</p>
     *
     * @param num
     *     An integer to be converted to
     * @return a {@link Float32} object
     */
    public static Float32 fromInteger(int num) {
        if (num == 0) {
            return Float32.Zero;
        }
        final boolean sign = num < 0;
        num = sign ? -num : num;
        int exponent = 0, significand = 0;
        for (int i = 30; i >= 0; i--) {
            if (((num >> i) & 1) == 1) {
                exponent = i + 127;
                significand = (num << (32 - i)) >>> 9;
                break;
            }
        }
        final int bits = ((sign) ? 0x80000000 : 0) | (exponent << 23) | significand;
        return new Float32(bits);
    }

    /**
     * <p>fromExact.</p>
     *
     * @param ef
     *     a {@link ExactFloat} object
     * @param e
     *     a {@link Environment} object
     * @return a {@link Float32} object
     */
    public static Float32 fromExact(final @NotNull ExactFloat ef, final @NotNull Environment e) {
        return Float32.Zero.fromExactFloat(ef, e);
    }

    /**
     * <p>exponent.</p>
     *
     * @return a int
     */
    public int exponent() {
        return ((this.bits >>> 23) & 0xFF) - 127;
    }

    /**
     * <p>negate.</p>
     *
     * @return a {@link Float32} object
     */
    @Override
    public @NotNull Float32 negate() {
        return new Float32(this.bits ^ 0x80000000); // Flip the sign bit
    }

    /**
     * <p>abs.</p>
     *
     * @return a {@link Float32} object
     */
    public @NotNull Float32 abs() {
        return new Float32(this.bits & 0x7FFFFFFF);
    }

    /**
     * <p>copySign.</p>
     *
     * @param signToTake
     *     a {@link Float32} object
     * @return a {@link Float32} object
     */
    public @NotNull Float32 copySign(final @NotNull Float32 signToTake) {
        return new Float32((this.bits & 0x7FFFFFFF) | (signToTake.bits & 0x80000000));
    }

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isSignMinus() {
        return (this.bits >>> 31) == 1;
    }

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isInfinite() {
        return this.exponent() == 128 && (this.bits & 0x007FFFFF) == 0;
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isNormal() {
        return this.exponent() != -127 && this.exponent() != 128;
    }

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isSubnormal() {
        return this.exponent() == -127 && !this.isZero();
    }

    // Section 6.2.1

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isNaN() {
        return this.exponent() == 128 && !this.isInfinite();
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
        return (this.bits & 0x400000) == 0;
    }

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    @Override
    public boolean isZero() {
        return this.bits == 0 || this.bits == 0x80000000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 NaN() {
        return Float32.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 Zero() {
        return Float32.Zero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 NegativeZero() {
        return Float32.NegativeZero;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 Infinity() {
        return Float32.Infinity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 NegativeInfinity() {
        return Float32.NegativeInfinity;
    }    // Some constants that allow fromExactFloat to be mostly copied

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Float32 fromExactFloat(@NotNull ExactFloat ef, final @NotNull Environment env) {
        if (ef.isZero()) {
            return ef.sign ? Float32.NegativeZero : Float32.Zero;
        }
        ef = ef.normalize();
        final int normalizedExponent = ef.exponent + ef.significand.bitLength();

        // Used to calculate how to round at the end
        final Float32 awayZero;
        final Float32 towardsZero;
        final BigInteger roundedBits;
        final int bitsToRound;

        if (normalizedExponent <= Float32.minexp + 1) {
            // Subnormal

            if (ef.exponent > Float32.minexp - Float32.sigbits) {
                assert ef.significand.bitLength() <= Float32.sigbits : "Its actually normal";
                return new Float32(
                    ef.sign, Float32.minexp,
                    ef.significand.shiftLeft(-(Float32.minexp - Float32.sigbits + 1) + ef.exponent).intValueExact()
                );
            }

            env.inexact = true;
            env.underflow = true; // Section 7.5
            bitsToRound = (Float32.minexp - Float32.sigbits + 1) - ef.exponent;
            final BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            towardsZero = new Float32(ef.sign, Float32.minexp, ef.significand.shiftRight(bitsToRound).intValueExact());
            final BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));
            if (upBits.testBit(0) || upBits.bitLength() <= Float32.sigbits) {
                assert upBits.bitLength() <= Float32.sigbits;
                awayZero = new Float32(ef.sign, Float32.minexp, upBits.intValueExact());
            } else {
                awayZero = new Float32(ef.sign, Float32.minexp + 1, upBits.intValueExact() & Float32.sigmask);
            }
        } else if (normalizedExponent > Float32.maxexp) {
            // Section 7.4
            env.overflow = true;
            env.inexact = true;
            switch (env.mode) {
                case ZERO:
                    return new Float32(ef.sign, Float32.maxexp - 1, -1); // Largest finite number
                case MIN:
                case MAX:
                    if (ef.sign != (env.mode == RoundingMode.MAX)) {
                        return ef.sign ? Float32.NegativeInfinity : Float32.Infinity;
                    } else {
                        return new Float32(ef.sign, Float32.maxexp - 1, -1); // Largest finite number
                    }
                case AWAY:
                case EVEN:
                    return ef.sign ? Float32.NegativeInfinity : Float32.Infinity;
            }
            assert false : "Not reachable";
            return ef.sign ? Float32.NegativeInfinity : Float32.Infinity;
        } else {
            if (ef.significand.bitLength() <= (Float32.sigbits + 1)) {
                // No rounding needed
                final var bitCount = ef.exponent + ef.significand.bitLength() - 1;
                assert bitCount > Float32.minexp : "Its actually subnormal";

                return new Float32(
                    ef.sign, bitCount,
                    ef.significand.shiftLeft((Float32.sigbits + 1) - ef.significand.bitLength())
                        .intValueExact() & Float32.sigmask
                );
            }
            env.inexact = true;
            bitsToRound = ef.significand.bitLength() - (Float32.sigbits + 1);
            final BigInteger mainBits = ef.significand.shiftRight(bitsToRound).shiftLeft(bitsToRound);
            roundedBits = ef.significand.subtract(mainBits);

            final BigInteger upBits = ef.significand.shiftRight(bitsToRound).add(BigInteger.valueOf(1));

            towardsZero = new Float32(
                ef.sign, ef.exponent + Float32.sigbits + bitsToRound,
                ef.significand.shiftRight(bitsToRound).intValueExact() & Float32.sigmask
            );
            if (upBits.testBit(0) || upBits.bitLength() <= Float32.sigbits + 1) {
                awayZero = new Float32(
                    ef.sign,
                    ef.exponent + Float32.sigbits + bitsToRound,
                    upBits.intValueExact() & Float32.sigmask
                );
            } else {
                awayZero = new Float32(
                    ef.sign, ef.exponent + (Float32.sigbits + 1) + bitsToRound,
                    upBits.shiftRight(1).intValueExact() & Float32.sigmask
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
            exponent = this.exponent() - 23;
            significand = BigInteger.valueOf((this.bits & 0x007FFFFF) + 0x00800000); // Add back the implied one
        } else if (this.isSubnormal()) {
            exponent = this.exponent() - 22;
            significand = BigInteger.valueOf(this.bits & 0x007FFFFF);
        } else {
            assert false : "This should not be reachable";
            // Dummy second
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
        return 30;
    }

    private static final int sigbits = 23, expbits = 8,
        maxexp = 1 << (Float32.expbits - 1),
        minexp = -(1 << (Float32.expbits - 1)) + 1,
        sigmask = (1 << Float32.sigbits) - 1;

}
