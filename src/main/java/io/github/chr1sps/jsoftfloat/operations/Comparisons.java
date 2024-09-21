package io.github.chr1sps.jsoftfloat.operations;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Floating;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Comparisons class.</p>
 */
public class Comparisons {

    private static <T extends Floating<T>> int compareNoNAN(@NotNull final T a, @NotNull final T b) {
        if (a.isZero()) {
            if (b.isZero()) {
                return 0;
            } else {
                return b.isSignMinus() ? 1 : -1;
            }
        }
        if (b.isZero()) {
            return a.isSignMinus() ? -1 : 1;
        }
        if (a.isInfinite()) {
            if (b.isInfinite() && a.isSignMinus() == b.isSignMinus()) {
                return 0;
            } else {
                return a.isSignMinus() ? 1 : -1;
            }
        }
        if (b.isInfinite()) {
            return b.isSignMinus() ? 1 : -1;
        }
        return a.toExactFloat().compareTo(b.toExactFloat());
    }

    private static <T extends Floating<T>> @NotNull T nonNaNmin(@NotNull final T a, @NotNull final T b) {
        // If signs are different it is easy
        // Also explicitly handles -0 vs +0
        if (a.isSignMinus() != b.isSignMinus()) {
            return (a.isSignMinus() ? a : b);
        }
        // Handle the infinite cases first
        if (a.isInfinite() || b.isInfinite()) {
            if (a.isInfinite() == a.isSignMinus()) {
                return a;
            } else {
                return b;
            }
        }
        if (Comparisons.compareNoNAN(a, b) <= 0) {
            return a;
        } else {
            return b;
        }
    }

    private static <T extends Floating<T>> @Nullable T handleNaN(@NotNull final T a, @NotNull final T b, final @NotNull Environment env) {
        // Section 5.3.1
        if (a.isNaN()) {
            if (b.isNaN()) {
                return b.NaN(); // Canonicalize in the case of two NaNs
            } else {
                return b;
            }
        }
        if (b.isNaN()) {
            return a;
        }
        return null;
    }

    private static <T extends Floating<T>> @Nullable T handleNaNNumber(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        // I think this handles the remaining cases of NaNs
        return Comparisons.handleNaN(a, b, env);
    }

    // minimum and minimumNumber are from the 201x revision

    /**
     * <p>minimum.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T minimum(@NotNull final T a, @NotNull final T b, final Environment env) {
        final T tmp = Comparisons.handleNaN(a, b, env);
        if (tmp != null)
            return tmp;
        return Comparisons.nonNaNmin(a, b);
    }

    /**
     * <p>maximum.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T maximum(@NotNull final T a, @NotNull final T b, final Environment env) {
        T tmp = Comparisons.handleNaN(a, b, env);
        if (tmp != null)
            return tmp;
        tmp = Comparisons.nonNaNmin(a, b);
        return (a == tmp) ? b : a; // flip for max rather than min
    }

    // Literally the same code as above, but with a different NaN handler

    /**
     * <p>minimumNumber.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T minimumNumber(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        final T tmp = Comparisons.handleNaNNumber(a, b, env);
        if (tmp != null)
            return tmp;
        return Comparisons.nonNaNmin(a, b);
    }

    /**
     * <p>maximumNumber.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T maximumNumber(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        T tmp = Comparisons.handleNaNNumber(a, b, env);
        if (tmp != null)
            return tmp;
        tmp = Comparisons.nonNaNmin(a, b);
        return (a == tmp) ? b : a; // flip for max rather than min
    }

    // Difference from minimumNumber explained by
    // https://freenode.logbot.info/riscv/20191012
    // > (TLDR: minNum(a, sNaN) == minNum(sNaN, a) == qNaN, whereas minimumNumber(a,
    // sNaN) == minimumNumber(sNaN, a) == a, where a is not NaN)

    /**
     * <p>minNum.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T minNum(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
            return a.NaN();
        }
        final T tmp = Comparisons.handleNaN(a, b, env);
        if (tmp != null)
            return tmp;
        return Comparisons.nonNaNmin(a, b);
    }

    /**
     * <p>maxNum.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a T object
     */
    public static <T extends Floating<T>> @NotNull T maxNum(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
            return a.NaN();
        }
        T tmp = Comparisons.handleNaN(a, b, env);
        if (tmp != null)
            return tmp;
        tmp = Comparisons.nonNaNmin(a, b);
        return (a == tmp) ? b : a; // flip for max rather than min
    }

    // All compares covered in Section 5.11

    /**
     * <p>compareQuietEqual.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietEqual(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        if (a.isNaN() || b.isNaN()) {
            return false;
        }
        return Comparisons.compareNoNAN(a, b) == 0;
    }

    /**
     * <p>equalSignaling.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean equalSignaling(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isNaN() || b.isNaN()) {
            env.invalid = true;
        }
        return Comparisons.compareQuietEqual(a, b, env);
    }

    /**
     * <p>compareQuietLessThan.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietLessThan(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        if (a.isNaN() || b.isNaN()) {
            return false;
        }
        return Comparisons.compareNoNAN(a, b) < 0;
    }

    /**
     * <p>compareSignalingLessThan.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareSignalingLessThan(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isNaN() || b.isNaN()) {
            env.invalid = true;
        }
        return Comparisons.compareQuietLessThan(a, b, env);
    }

    /**
     * <p>compareQuietLessThanEqual.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietLessThanEqual(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        if (a.isNaN() || b.isNaN()) {
            return false;
        }
        return Comparisons.compareNoNAN(a, b) <= 0;
    }

    /**
     * <p>compareSignalingLessThanEqual.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareSignalingLessThanEqual(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isNaN() || b.isNaN()) {
            env.invalid = true;
        }
        return Comparisons.compareQuietLessThanEqual(a, b, env);
    }

    /**
     * <p>compareQuietGreaterThan.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietGreaterThan(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        if (a.isNaN() || b.isNaN()) {
            return false;
        }
        return Comparisons.compareNoNAN(a, b) > 0;
    }

    /**
     * <p>compareSignalingGreaterThan.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareSignalingGreaterThan(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isNaN() || b.isNaN()) {
            env.invalid = true;
        }
        return Comparisons.compareQuietGreaterThan(a, b, env);
    }

    /**
     * <p>compareQuietGreaterThanEqual.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietGreaterThanEqual(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        if (a.isNaN() || b.isNaN()) {
            return false;
        }
        return Comparisons.compareNoNAN(a, b) >= 0;
    }

    /**
     * <p>compareSignalingGreaterThanEqual.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareSignalingGreaterThanEqual(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isNaN() || b.isNaN()) {
            env.invalid = true;
        }
        return Comparisons.compareQuietGreaterThanEqual(a, b, env);
    }

    /**
     * <p>compareQuietUnordered.</p>
     *
     * @param a   a T object
     * @param b   a T object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @param <T> a T class
     * @return a boolean
     */
    public static <T extends Floating<T>> boolean compareQuietUnordered(@NotNull final T a, @NotNull final T b, @NotNull final Environment env) {
        if (a.isSignalling() || b.isSignalling()) {
            env.invalid = true;
        }
        return a.isNaN() || b.isNaN();
    }
}
