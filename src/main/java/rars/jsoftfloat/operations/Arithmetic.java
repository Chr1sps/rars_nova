package rars.jsoftfloat.operations;

import org.jetbrains.annotations.NotNull;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.RoundingMode;
import rars.jsoftfloat.internal.ExactFloat;
import rars.jsoftfloat.types.Floating;

/**
 * Groups any arithmetic operations such as addition, subtraction, etc
 */
public final class Arithmetic {
    private Arithmetic() {
    }

    /**
     * <p>add.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T add(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;
        if (b.isNaN())
            return b;

        // Section 6.1 and 7.2
        if (a.isInfinite()) {
            if (b.isInfinite() && (b.isSignMinus() != a.isSignMinus())) {
                env.invalid = true;
                return a.NaN(); // inf - inf is undefined
            } else {
                return a;
            }
        } else if (b.isInfinite()) {
            return b;
        }

        // Section 6.3
        if (a.isZero()) {
            if (b.isZero()) {
                if (a.isSignMinus() == b.isSignMinus()) {
                    return a; // They are the same, just pick one
                } else {
                    // Explicitly stated in the spec
                    return (env.mode == RoundingMode.MIN) ? a.NegativeZero() : a.Zero();
                }
            } else {
                return b;
            }
        } else if (b.isZero()) {
            return a;
        }

        final ExactFloat out = (a.toExactFloat()).add(b.toExactFloat());
        // Check to see if it was x + (-x)
        if (out.isZero()) {
            return (env.mode == RoundingMode.MIN) ? a.NegativeZero() : a.Zero();
        }
        return a.fromExactFloat(out, env);
    }

    /**
     * <p>subtraction.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T subtraction(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;
        if (b.isNaN())
            return b;

        // After this it is equivalent to adding a negative
        return Arithmetic.add(a, b.negate(), env);
    }

    /**
     * <p>multiplication.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T multiplication(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;
        if (b.isNaN())
            return b;

        // Section 7.2
        if ((a.isZero() && b.isInfinite()) || (b.isZero() && a.isInfinite())) {
            env.invalid = true;
            return a.NaN();
        }

        // Section 6.1
        if (a.isInfinite() || b.isInfinite()) {
            return a.isSignMinus() == b.isSignMinus() ? a.Infinity() : a.NegativeInfinity();
        }

        if (a.isZero() || b.isZero()) {
            return a.isSignMinus() == b.isSignMinus() ? a.Zero() : a.NegativeZero();
        }

        return a.fromExactFloat(a.toExactFloat().multiply(b.toExactFloat()), env);
    }

    /**
     * <p>squareRoot.</p>
     *
     * @param a   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T squareRoot(@NotNull final T a, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;

        // Section 6.3 or Section 5.4.1
        if (a.isZero()) {
            return a;
        }

        // Section 7.2
        if (a.isSignMinus()) {
            env.invalid = true;
            return a.NaN();
        }

        // Section 6.1
        if (a.isInfinite()) {
            return a;
        }

        return a.fromExactFloat(a.toExactFloat().squareRoot(a.maxPrecision()), env);
    }

    /**
     * <p>fusedMultiplyAdd.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param c   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T fusedMultiplyAdd(@NotNull final T a, @NotNull final T b, @NotNull final T c, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;
        if (b.isNaN())
            return b;
        // This behaviour is implementation defined - Section 7.2
        if (c.isNaN())
            return c;

        // Section 7.2
        if ((a.isZero() && b.isInfinite()) || (b.isZero() && a.isInfinite())) {
            env.invalid = true;
            return a.NaN();
        }

        // Section 6.1
        if (a.isInfinite() || b.isInfinite()) {
            return Arithmetic.add(a.isSignMinus() == b.isSignMinus() ? a.Infinity() : a.NegativeInfinity(), c, env);
        }

        if (a.isZero() || b.isZero()) {
            return Arithmetic.add(a.isSignMinus() == b.isSignMinus() ? a.Zero() : a.NegativeZero(), c, env);
        }

        final ExactFloat multiplication = a.toExactFloat().multiply(b.toExactFloat());

        return a.fromExactFloat(multiplication.add(c.toExactFloat()), env);
    }

    /**
     * <p>division.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T division(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        // TODO: handle signalling correctly

        // Section 6.2
        if (a.isNaN())
            return a;
        if (b.isNaN())
            return b;

        // Section 7.2
        if ((a.isZero() && b.isZero()) || (a.isInfinite() && b.isInfinite())) {
            env.invalid = true;
            return a.NaN();
        }

        // Section 6.1
        if (a.isInfinite()) {
            return (a.isSignMinus() == b.isSignMinus()) ? a.Infinity() : a.NegativeInfinity();
        }

        if (b.isInfinite() || a.isZero()) {
            return (a.isSignMinus() == b.isSignMinus()) ? a.Zero() : a.NegativeZero();
        }

        // Section 7.3
        if (b.isZero()) {
            env.divByZero = true;
            return (a.isSignMinus() == b.isSignMinus()) ? a.Infinity() : a.NegativeInfinity();
        }

        assert a.isFinite() && b.isFinite() : "Both should definitely be finite by this point";

        // TODO: in tie cases round away from zero despite rounding mode unless actually
        // precise
        return a.fromExactFloat(a.toExactFloat().divide(b.toExactFloat(), a.maxPrecision()), env);
    }
}
