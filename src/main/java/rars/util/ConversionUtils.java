package rars.util;

/**
 * Utility class for converting between numerical types. An extension of
 * some of the pre-existing methods in the Java standard library. Some of
 * these methods are meant to convey an explicit conversion, so if the
 * implementation looks something like `return (int) value`, it is meant
 * to be this way.
 */
public final class ConversionUtils {
    private ConversionUtils() {
    }

    /**
     * Returns the lower 32 bits of a long as an integer.
     *
     * @param value
     *     the long to convert
     * @return the lower 32 bits of the long as an integer
     */
    public static int longLowerHalfToInt(final long value) {
        return Long.valueOf(value & 0xFFFF_FFFFL).intValue();
    }

    /**
     * Returns the upper 32 bits of a long as an integer.
     *
     * @param value
     *     the long to convert
     * @return the upper 32 bits of the long as an integer
     */
    public static int longUpperHalfToInt(final long value) {
        return Long.valueOf(value >>> 32).intValue();
    }
}
