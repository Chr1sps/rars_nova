package rars.ieee754

/** The different ways that rounding can be performed */
enum class RoundingMode {
    /** Round to nearest; in event of a tie, round towards even. */
    EVEN,

    /** Round to nearest; in event of a tie, round away from zero. */
    AWAY,

    /** Round towards -Infinity. */
    MIN,

    /** Round towards +Infinity. */
    MAX,

    /** Round towards zero. */
    ZERO
}
