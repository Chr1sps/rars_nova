package io.github.chr1sps.jsoftfloat.types;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.internal.ExactFloat;
import org.jetbrains.annotations.NotNull;

/**
 * General classifications that any floating point class needs to provide.
 *
 * @param <T>
 */
public interface Floating<T extends Floating<T>> {
    // TODO: order/group these

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    boolean isSignMinus();

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    boolean isInfinite();

    /**
     * <p>isFinite.</p>
     *
     * @return a boolean
     */
    default boolean isFinite() {
        return this.isZero() || this.isNormal() || this.isSubnormal();
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    boolean isNormal();

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    boolean isSubnormal();

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    boolean isNaN();

    /**
     * <p>isSignalling.</p>
     *
     * @return a boolean
     */
    boolean isSignalling();

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    boolean isZero();

    // TODO: consider making a full bit field representation method for generic
    // conversions

    /**
     * <p>maxPrecision.</p>
     *
     * @return a int
     */
    int maxPrecision();

    /**
     * <p>NaN.</p>
     *
     * @return a T object
     */
    @NotNull T NaN();

    /**
     * <p>Zero.</p>
     *
     * @return a T object
     */
    @NotNull T Zero();

    /**
     * <p>NegativeZero.</p>
     *
     * @return a T object
     */
    @NotNull T NegativeZero();

    /**
     * <p>Infinity.</p>
     *
     * @return a T object
     */
    @NotNull T Infinity();

    /**
     * <p>NegativeInfinity.</p>
     *
     * @return a T object
     */
    @NotNull T NegativeInfinity();

    /**
     * <p>fromExactFloat.</p>
     *
     * @param f   a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a T object
     */
    @NotNull T fromExactFloat(@NotNull ExactFloat f, @NotNull Environment env);

    /**
     * <p>toExactFloat.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    @NotNull ExactFloat toExactFloat();

    /**
     * <p>negate.</p>
     *
     * @return a T object
     */
    @NotNull T negate();

}
