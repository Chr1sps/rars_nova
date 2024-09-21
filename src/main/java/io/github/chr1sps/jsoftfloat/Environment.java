package io.github.chr1sps.jsoftfloat;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Environment class.</p>
 */
public class Environment {
    public @NotNull RoundingMode mode;

    /**
     * Triggered when a result differs from what would have been computed were both
     * exponent range and precision unbounded
     * <p>
     * For example, it would be triggered by 1/3
     */
    public boolean inexact = false;
    /**
     * Triggered when a result is tiny (|result| &lt; b^emin; smaller than the smallest
     * normal number)
     */
    // TODO: handling of underflow was incorrect up till this point subnormal
    // numbers result in underflow
    public boolean underflow = false;
    /**
     * Triggered if a result is larger than the largest finite representable number
     */
    public boolean overflow = false;
    /**
     * Triggered by creating an infinite number using zero
     * <p>
     * For example, it would be triggered by 1/0 or log(0)
     */
    public boolean divByZero = false;
    /**
     * Triggered when an operation produces no meaningful value
     * <p>
     * For example, it would be triggered by 0/0, Infinity - Infinity, etc
     */
    public boolean invalid = false;


    /**
     * <p>Constructor for Environment.</p>
     *
     * @param mode a {@link io.github.chr1sps.jsoftfloat.RoundingMode} object
     */
    public Environment(final @NotNull RoundingMode mode) {
        this.mode = mode;
    }

    /**
     * <p>Constructor for Environment.</p>
     */
    public Environment() {
        this(RoundingMode.even);
    }
}
