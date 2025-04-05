package rars.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Some utility methods for working with binary representations.
 *
 * @author Pete Sanderson, Ken Vollmar, and Jason Bumgarner
 * @version July 2005
 */
@Deprecated
public final class BinaryUtilsOld {

    // Using int value 0-15 as index, yields equivalent hex digit as char.
    private static final char[] chars = {
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f'
    };
    // Use this to produce String equivalent of unsigned int value (add it to int
    // value, result is long)
    private static final long UNSIGNED_BASE = 0x1_0000_0000L;

    private BinaryUtilsOld() {
    }

    /**
     * Translate String consisting of '1's and '0's into an int value having that
     * binary representation.
     * The String is assumed to be at most 32 characters long. No error checking is
     * performed.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String value to convert.
     * @return int whose binary value corresponds to decoded String.
     */
    public static int binaryStringToInt(final @NotNull String value) {
        int result = value.charAt(0) - 48;
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - 48);
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into a long value having that
     * binary representation.
     * The String is assumed to be at most 64 characters long. No error checking is
     * performed.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String value to convert.
     * @return long whose binary value corresponds to decoded String.
     */
    private static long binaryStringToLong(final @NotNull String value) {
        long result = value.charAt(0) - 48;
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - 48);
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into String equivalent of the
     * corresponding
     * hexadecimal value. No length limit.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String value to convert.
     * @return String containing '0', '1', ...'F' characters which form hexadecimal
     * equivalent of decoded String.
     */
    @Contract("_ -> new")
    public static @NotNull String binaryStringToHexString(final @NotNull String value) {
        final int digits = (value.length() + 3) / 4;
        final char[] hexChars = new char[digits + 2];
        hexChars[0] = '0';
        hexChars[1] = 'x';
        int position = value.length() - 1;
        for (int digs = 0; digs < digits; digs++) {
            int result = 0;
            int pow = 1;
            int rep = 0;
            while (rep < 4 && position >= 0) {
                if (value.charAt(position) == '1') {
                    result = result + pow;
                }
                pow *= 2;
                position--;
                rep++;
            }
            hexChars[digits - digs + 1] = BinaryUtilsOld.chars[result];
        }
        return new String(hexChars);
    }

    /**
     * Translate String consisting of hexadecimal digits into String consisting of
     * corresponding binary digits ('1's and '0's). No length limit.
     * String position 0 will have most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     String containing '0', '1', ...'f'
     *     characters which form hexadecimal. Letters may be either upper
     *     or lower case.
     *     Works either with or without leading "Ox".
     * @return String with equivalent value in binary.
     */
    public static @NotNull String hexStringToBinaryString(@NotNull String value) {
        // slice off leading Ox or 0X
        if (value.indexOf("0x") == 0 || value.indexOf("0X") == 0) {
            value = value.substring(2);
        }
        final StringBuilder result = new StringBuilder();
        for (int digs = 0; digs < value.length(); digs++) {
            switch (value.charAt(digs)) {
                case '0':
                    result.append("0000");
                    break;
                case '1':
                    result.append("0001");
                    break;
                case '2':
                    result.append("0010");
                    break;
                case '3':
                    result.append("0011");
                    break;
                case '4':
                    result.append("0100");
                    break;
                case '5':
                    result.append("0101");
                    break;
                case '6':
                    result.append("0110");
                    break;
                case '7':
                    result.append("0111");
                    break;
                case '8':
                    result.append("1000");
                    break;
                case '9':
                    result.append("1001");
                    break;
                case 'a':
                case 'A':
                    result.append("1010");
                    break;
                case 'b':
                case 'B':
                    result.append("1011");
                    break;
                case 'c':
                case 'C':
                    result.append("1100");
                    break;
                case 'd':
                case 'D':
                    result.append("1101");
                    break;
                case 'e':
                case 'E':
                    result.append("1110");
                    break;
                case 'f':
                case 'F':
                    result.append("1111");
                    break;
            }
        }
        return result.toString();
    }

    /**
     * Produce String equivalent of integer value interpreting it as an unsigned
     * integer.
     * For instance, -1 (0xffffffff) produces "4294967295" instead of "-1".
     *
     * @param d
     *     The int value to interpret.
     * @return String which forms unsigned 32 bit equivalent of int.
     */
    public static @NotNull String unsignedIntToIntString(final int d) {
        return (d >= 0) ? Integer.toString(d) : Long.toString(BinaryUtilsOld.UNSIGNED_BASE + d);
    }

    /**
     * Returns int representing the bit values of the high order 32 bits of given
     * 64 bit long value.
     *
     * @param longValue
     *     The long value from which to extract bits.
     * @return int containing high order 32 bits of argument
     */
    public static int highOrderLongToInt(final long longValue) {
        return (int) (longValue >> 32); // high order 32 bits
    }

    /**
     * Returns int representing the bit values of the low order 32 bits of given
     * 64 bit long value.
     *
     * @param longValue
     *     The long value from which to extract bits.
     * @return int containing low order 32 bits of argument
     */
    public static int lowOrderLongToInt(final long longValue) {
        return (int) (longValue << 32 >> 32); // low order 32 bits
    }

    /**
     * Returns the bit value of the given bit position of the given int value.
     *
     * @param value
     *     The value to read the bit from.
     * @param bit
     *     bit position in range 0 (least significant) to 31 (most)
     * @return 0 if the bit position contains 0, and 1 otherwise.
     */
    public static int bitValue(final int value, final int bit) {
        return 1 & (value >> bit);
    }

}
