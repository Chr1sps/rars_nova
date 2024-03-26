package io.github.chr1sps.jsoftfloat.operations;

import java.math.BigInteger;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.Flags;
import io.github.chr1sps.jsoftfloat.internal.ExactFloat;
import io.github.chr1sps.jsoftfloat.types.Floating;

/**
 * Groups conversion operations such as integer to float32, float32 to integer,
 * etc
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class Conversions {
    /**
     * <p>roundToIntegral.</p>
     *
     * @param f   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> T roundToIntegral(T f, Environment env) {
        // Section 5.9 and 7.2
        if (f.isNaN()) {
            // TODO: signal invalid operation
            return f;
        }
        if (f.isInfinite()) {
            // TODO: handle correctly
            return f;
        }
        if (f.isZero()) {
            return f;
        }
        return f.fromExactFloat(f.toExactFloat().roundToIntegral(env), env);
    }

    /**
     * <p>convertToIntegral.</p>
     *
     * @param f     a T object
     * @param max   a {@link java.math.BigInteger} object
     * @param min   a {@link java.math.BigInteger} object
     * @param env   a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a {@link java.math.BigInteger} object
     */
    public static <T extends Floating<T>> BigInteger convertToIntegral(T f, BigInteger max, BigInteger min,
                                                                       Environment env, boolean quiet) {
        // Section 5.9 and 7.2
        if (f.isNaN()) {
            env.flags.add(Flags.invalid);
            return max;
        }

        if (f.isInfinite()) {
            env.flags.add(Flags.invalid);
            return f.isSignMinus() ? min : max;
        }

        Environment copy = new Environment();
        copy.mode = env.mode;
        BigInteger rounded;
        if (f.isZero()) {
            rounded = BigInteger.ZERO;
        } else {
            rounded = f.toExactFloat().toIntegral(env);
        }

        // Section 5.8
        if (rounded.compareTo(max) > 0 || rounded.compareTo(min) < 0) {
            env.flags.add(Flags.invalid);
        } else if (!quiet && copy.flags.contains(Flags.inexact)) {
            env.flags.add(Flags.inexact);
        }
        return rounded.min(max).max(min); // clamp rounded to between max and min
    }

    /**
     * <p>convertToInt.</p>
     *
     * @param f   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToInt(T f, Environment env) {
        return convertToInt(f, env, false);
    }

    /**
     * <p>convertToInt.</p>
     *
     * @param f     a T object
     * @param env   a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToInt(T f, Environment env, boolean quiet) {
        BigInteger rounded = convertToIntegral(f, BigInteger.valueOf(Integer.MAX_VALUE),
                BigInteger.valueOf(Integer.MIN_VALUE), env, quiet);
        return rounded.intValueExact();
    }

    /**
     * <p>convertToUnsignedInt.</p>
     *
     * @param f     a T object
     * @param env   a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToUnsignedInt(T f, Environment env, boolean quiet) {
        BigInteger rounded = convertToIntegral(f, BigInteger.valueOf(0xFFFFFFFFL), BigInteger.ZERO, env, quiet);
        return (int) (rounded.longValueExact() & 0xFFFFFFFFL);
    }

    /**
     * <p>convertToLong.</p>
     *
     * @param f     a T object
     * @param env   a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a long
     */
    public static <T extends Floating<T>> long convertToLong(T f, Environment env, boolean quiet) {
        BigInteger rounded = convertToIntegral(f, BigInteger.valueOf(Long.MAX_VALUE),
                BigInteger.valueOf(Long.MIN_VALUE), env, quiet);
        return rounded.longValueExact();
    }

    /**
     * <p>convertToUnsignedLong.</p>
     *
     * @param f     a T object
     * @param env   a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a long
     */
    public static <T extends Floating<T>> long convertToUnsignedLong(T f, Environment env, boolean quiet) {
        BigInteger rounded = convertToIntegral(f, BigInteger.valueOf(-1).add(BigInteger.ONE.shiftLeft(64)),
                BigInteger.ZERO, env, quiet);
        return rounded.longValue();
    }

    /**
     * <p>convertFromInt.</p>
     *
     * @param i      a {@link java.math.BigInteger} object
     * @param env    a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param helper a T object
     * @param <T>    a T class
     * @return a T object
     */
    public static <T extends Floating<T>> T convertFromInt(BigInteger i, Environment env, T helper) {
        if (i.equals(BigInteger.ZERO)) {
            return helper.Zero();
        }
        return helper.fromExactFloat(new ExactFloat(i), env);
    }
}
