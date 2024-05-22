package io.github.chr1sps.jsoftfloat.types;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.internal.ExactFloat;

/**
 * General classifications that any floating point class needs to provide.
 *
 * @param <T>
 */
public abstract class Floating<T extends Floating<T>> {
    // TODO: order/group these

    /**
     * <p>isSignMinus.</p>
     *
     * @return a boolean
     */
    public abstract boolean isSignMinus();

    /**
     * <p>isInfinite.</p>
     *
     * @return a boolean
     */
    public abstract boolean isInfinite();

    /**
     * <p>isFinite.</p>
     *
     * @return a boolean
     */
    public boolean isFinite() {
        return isZero() || isNormal() || isSubnormal();
    }

    /**
     * <p>isNormal.</p>
     *
     * @return a boolean
     */
    public abstract boolean isNormal();

    /**
     * <p>isSubnormal.</p>
     *
     * @return a boolean
     */
    public abstract boolean isSubnormal();

    /**
     * <p>isNaN.</p>
     *
     * @return a boolean
     */
    public abstract boolean isNaN();

    /**
     * <p>isSignalling.</p>
     *
     * @return a boolean
     */
    public abstract boolean isSignalling();

    /**
     * <p>isCanonical.</p>
     *
     * @return a boolean
     */
    public abstract boolean isCanonical();

    /**
     * <p>isZero.</p>
     *
     * @return a boolean
     */
    public abstract boolean isZero();

    // TODO: consider making a full bit field representation method for generic
    // conversions

    /**
     * <p>maxPrecision.</p>
     *
     * @return a int
     */
    public abstract int maxPrecision();

    /**
     * <p>NaN.</p>
     *
     * @return a T object
     */
    public abstract T NaN();

    /**
     * <p>Zero.</p>
     *
     * @return a T object
     */
    public abstract T Zero();

    /**
     * <p>NegativeZero.</p>
     *
     * @return a T object
     */
    public abstract T NegativeZero();

    /**
     * <p>Infinity.</p>
     *
     * @return a T object
     */
    public abstract T Infinity();

    /**
     * <p>NegativeInfinity.</p>
     *
     * @return a T object
     */
    public abstract T NegativeInfinity();

    /**
     * <p>fromExactFloat.</p>
     *
     * @param f   a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     * @param env a {@link io.github.chr1sps.jsoftfloat.Environment} object
     * @return a T object
     */
    public abstract T fromExactFloat(ExactFloat f, Environment env);

    /**
     * <p>toExactFloat.</p>
     *
     * @return a {@link io.github.chr1sps.jsoftfloat.internal.ExactFloat} object
     */
    public abstract ExactFloat toExactFloat();

    /**
     * <p>negate.</p>
     *
     * @return a T object
     */
    public abstract T negate();

}
