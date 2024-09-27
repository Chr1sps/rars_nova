package rars.jsoftfloat.operations;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.internal.ExactFloat;
import rars.jsoftfloat.types.Floating;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Groups conversion operations such as integer to float32, float32 to integer,
 * etc
 */
public final class Conversions {
    private Conversions() {
    }

    /**
     * <p>roundToIntegral.</p>
     *
     * @param f   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T roundToIntegral(@NotNull final T f, @NotNull final Environment env) {
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
     * @param env   a {@link Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a {@link java.math.BigInteger} object
     */
    public static <T extends Floating<T>> BigInteger convertToIntegral(@NotNull final T f, final BigInteger max, final BigInteger min,
                                                                       @NotNull final Environment env, final boolean quiet) {
        // Section 5.9 and 7.2
        if (f.isNaN()) {
            env.invalid = true;
            return max;
        }

        if (f.isInfinite()) {
            env.invalid = true;
            return f.isSignMinus() ? min : max;
        }

        final Environment copy = new Environment();
        copy.mode = env.mode;
        final BigInteger rounded;
        if (f.isZero()) {
            rounded = BigInteger.ZERO;
        } else {
            rounded = f.toExactFloat().toIntegral(env);
        }

        // Section 5.8
        if (rounded.compareTo(max) > 0 || rounded.compareTo(min) < 0) {
            env.invalid = true;
        } else if (!quiet && copy.inexact) {
            env.inexact = true;
        }
        return rounded.min(max).max(min); // clamp rounded to between max and min
    }

    /**
     * <p>convertToInt.</p>
     *
     * @param f   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToInt(@NotNull final T f, @NotNull final Environment env) {
        return Conversions.convertToInt(f, env, false);
    }

    /**
     * <p>convertToInt.</p>
     *
     * @param f     a T object
     * @param env   a {@link Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToInt(@NotNull final T f, @NotNull final Environment env, final boolean quiet) {
        final BigInteger rounded = Conversions.convertToIntegral(f, BigInteger.valueOf(Integer.MAX_VALUE),
                BigInteger.valueOf(Integer.MIN_VALUE), env, quiet);
        return rounded.intValueExact();
    }

    /**
     * <p>convertToUnsignedInt.</p>
     *
     * @param f     a T object
     * @param env   a {@link Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a int
     */
    public static <T extends Floating<T>> int convertToUnsignedInt(@NotNull final T f, @NotNull final Environment env, final boolean quiet) {
        final BigInteger rounded = Conversions.convertToIntegral(f, BigInteger.valueOf(0xFFFFFFFFL), BigInteger.ZERO, env, quiet);
        return (int) (rounded.longValueExact() & 0xFFFFFFFFL);
    }

    /**
     * <p>convertToLong.</p>
     *
     * @param f     a T object
     * @param env   a {@link Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a long
     */
    public static <T extends Floating<T>> long convertToLong(@NotNull final T f, @NotNull final Environment env, final boolean quiet) {
        final BigInteger rounded = Conversions.convertToIntegral(f, BigInteger.valueOf(Long.MAX_VALUE),
                BigInteger.valueOf(Long.MIN_VALUE), env, quiet);
        return rounded.longValueExact();
    }

    /**
     * <p>convertToUnsignedLong.</p>
     *
     * @param f     a T object
     * @param env   a {@link Environment} object
     * @param quiet a boolean
     * @param <T>   a T class
     * @return a long
     */
    public static <T extends Floating<T>> long convertToUnsignedLong(@NotNull final T f, @NotNull final Environment env, final boolean quiet) {
        final BigInteger rounded = Conversions.convertToIntegral(f, BigInteger.valueOf(-1).add(BigInteger.ONE.shiftLeft(64)),
                BigInteger.ZERO, env, quiet);
        return rounded.longValue();
    }

    /**
     * <p>convertFromInt.</p>
     *
     * @param i      a {@link java.math.BigInteger} object
     * @param env    a {@link Environment} object
     * @param helper a T object
     * @param <T>    a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T convertFromInt(@NotNull final BigInteger i, final @NotNull Environment env, @NotNull final T helper) {
        if (i.equals(BigInteger.ZERO)) {
            return helper.Zero();
        }
        return helper.fromExactFloat(new ExactFloat(i), env);
    }
}
