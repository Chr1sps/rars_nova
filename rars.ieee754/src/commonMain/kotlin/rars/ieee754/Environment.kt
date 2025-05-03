package rars.ieee754

/** Represents the environment in which floating point computations are performed. */
class Environment(var mode: RoundingMode = RoundingMode.EVEN) {
    /**
     * Triggered when a result differs from what would have been computed were
     * both exponent range and precision unbounded. For example, it would be
     * triggered by 1/3.
     */
    var inexact: Boolean = false

    /**
     * Triggered when a result is tiny (|result| < b^emin; smaller than
     * the smallest normal number).
     */
    var underflow: Boolean = false

    /** Triggered if a result is larger than the largest finite representable number. */
    var overflow: Boolean = false

    /**
     * Triggered by creating an infinite number using zero.
     * For example, it would be triggered by 1/0 or log(0).
     */
    var divByZero: Boolean = false

    /**
     * Triggered when an operation produces no meaningful value.
     * For example, it would be triggered by 0/0, Infinity - Infinity, etc.
     */
    var invalid: Boolean = false
}
