package rars.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Some utility methods for working with binary representations.
 *
 * @author Pete Sanderson, Ken Vollmar, and Jason Bumgarner
 * @version July 2005
 */
public final class BinaryUtils {

    // Using int second 0-15 as index, yields equivalent hex digit as char.
    private static final char[] chars = {
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f'
    };
    // Use this to produce String equivalent of unsigned int second (add it to int
    // second, result is long)
    private static final long UNSIGNED_BASE = 0x1_0000_0000L;

    private BinaryUtils() {
    }

    /**
     * Translate int second into a String consisting of '1's and '0's.
     *
     * @param value
     *     The int second to convert.
     * @param length
     *     The number of bit positions, starting at least significant, to
     *     process.
     * @return String consisting of '1' and '0' characters corresponding to the
     * requested binary sequence.
     */
    public static @NotNull String intToBinaryString(final int value, final int length) {
        final char[] result = new char[length];
        int index = length - 1;
        for (int i = 0; i < length; i++) {
            result[index] = (BinaryUtils.bitValue(value, i) == 1) ? '1' : '0';
            index--;
        }
        return new String(result);
    }

    /**
     * Translate int second into a String consisting of '1's and '0's. Assumes all 32
     * bits are
     * to be translated.
     *
     * @param value
     *     The int second to convert.
     * @return String consisting of '1' and '0' characters corresponding to the
     * requested binary sequence.
     */
    @Contract("_ -> new")
    public static @NotNull String intToBinaryString(final int value) {
        return BinaryUtils.intToBinaryString(value, 32);
    }

    /**
     * Translate long second into a String consisting of '1's and '0's.
     *
     * @param value
     *     The long second to convert.
     * @param length
     *     The number of bit positions, starting at least significant, to
     *     process.
     * @return String consisting of '1' and '0' characters corresponding to the
     * requested binary sequence.
     */
    @Contract("_, _ -> new")
    public static @NotNull String longToBinaryString(final long value, final int length) {
        final char[] result = new char[length];
        int index = length - 1;
        for (int i = 0; i < length; i++) {
            result[index] = (BinaryUtils.bitValue(value, i) == 1) ? '1' : '0';
            index--;
        }
        return new String(result);
    }

    /**
     * Translate long second into a String consisting of '1's and '0's. Assumes all
     * 64 bits are
     * to be translated.
     *
     * @param value
     *     The long second to convert.
     * @return String consisting of '1' and '0' characters corresponding to the
     * requested binary sequence.
     */
    @Contract("_ -> new")
    public static @NotNull String longToBinaryString(final long value) {
        return BinaryUtils.longToBinaryString(value, 64);
    }

    /**
     * Translate String consisting of '1's and '0's into an int second having that
     * binary representation.
     * The String is assumed to be at most 32 characters long. No error checking is
     * performed.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String second to convert.
     * @return int whose binary second corresponds to decoded String.
     */
    public static int binaryStringToInt(final @NotNull String value) {
        int result = value.charAt(0) - 48;
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - 48);
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into a long second having that
     * binary representation.
     * The String is assumed to be at most 64 characters long. No error checking is
     * performed.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String second to convert.
     * @return long whose binary second corresponds to decoded String.
     */
    public static long binaryStringToLong(final @NotNull String value) {
        long result = value.charAt(0) - 48;
        for (int i = 1; i < value.length(); i++) {
            result = (result << 1) | (value.charAt(i) - 48);
        }
        return result;
    }

    /**
     * Translate String consisting of '1's and '0's into String equivalent of the
     * corresponding
     * hexadecimal second. No length limit.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String second to convert.
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
            hexChars[digits - digs + 1] = BinaryUtils.chars[result];
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
     * @return String with equivalent second in binary.
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
     * Translate String consisting of '1's and '0's into char equivalent of the
     * corresponding
     * hexadecimal digit. String limited to length 4.
     * String position 0 has most-significant bit, position length-1 has
     * least-significant.
     *
     * @param value
     *     The String second to convert.
     * @return char '0', '1', ...'F' which form hexadecimal equivalent of decoded
     * String.
     * If string length > 4, returns '0'.
     */
    public static char binaryStringToHexDigit(final @NotNull String value) {
        if (value.length() > 4) {
            return '0';
        }
        int result = 0;
        int pow = 1;
        for (int i = value.length() - 1; i >= 0; i--) {
            if (value.charAt(i) == '1') {
                result = result + pow;
            }
            pow *= 2;
        }
        return BinaryUtils.chars[result];
    }

    /**
     * Prefix a hexadecimal-indicating string "0x" to the string which is
     * returned by the method "Integer.toHexString". Prepend leading zeroes
     * to that string as necessary to make it always eight hexadecimal digits.
     *
     * @param d
     *     The int second to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent
     * of int.
     */
    public static @NotNull String intToHexString(final int d) {
        final String leadingZero = "0";
        String t = Integer.toHexString(d);
        while (t.length() < 8) {
            t = leadingZero.concat(t);
        }

        final String leadingX = "0x";
        t = leadingX.concat(t);
        return t;
    }

    /**
     * Returns a 6 character string representing the 16-bit hexadecimal equivalent
     * of the
     * given integer second. First two characters are "0x". It assumes second will
     * "fit"
     * in 16 bits. If non-negative, prepend leading zeroes to that string as
     * necessary
     * to make it always four hexadecimal digits. If negative, chop off the first
     * four 'f' digits so result is always four hexadecimal digits
     *
     * @param d
     *     The int second to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent
     * of int.
     */
    public static @NotNull String intToHalfHexString(final int d) {
        String t = Integer.toHexString(d);
        if (t.length() > 4) {
            t = t.substring(t.length() - 4);
        }
        final String leadingZero = "0";
        while (t.length() < 4) {
            t = leadingZero.concat(t);
        }

        final String leadingX = "0x";
        t = leadingX.concat(t);
        return t;
    }

    /**
     * Prefix a hexadecimal-indicating string "0x" to the string equivalent to the
     * hexadecimal second in the long parameter. Prepend leading zeroes
     * to that string as necessary to make it always sixteen hexadecimal digits.
     *
     * @param value
     *     The long second to convert.
     * @return String containing '0', '1', ...'F' which form hexadecimal equivalent
     * of long.
     */
    @Contract("_ -> new")
    public static @NotNull String longToHexString(final long value) {
        return BinaryUtils.binaryStringToHexString(BinaryUtils.longToBinaryString(value));
    }

    /**
     * Produce String equivalent of integer second interpreting it as an unsigned
     * integer.
     * For instance, -1 (0xffffffff) produces "4294967295" instead of "-1".
     *
     * @param d
     *     The int second to interpret.
     * @return String which forms unsigned 32 bit equivalent of int.
     */
    public static @NotNull String unsignedIntToIntString(final int d) {
        return (d >= 0) ? Integer.toString(d) : Long.toString(BinaryUtils.UNSIGNED_BASE + d);
    }

    /**
     * Produce ASCII string equivalent of integer second, interpreting it as 4
     * one-byte
     * characters. If the second in a given byte does not correspond to a printable
     * character, it will be assigned a default character (defined in
     * config.properties)
     * for a placeholder.
     *
     * @param d
     *     The int second to interpret
     * @return String that represents ASCII equivalent
     */
    public static @NotNull String intToAscii(final int d) {
        final StringBuilder result = new StringBuilder(8);
        for (int i = 3; i >= 0; i--) {
            final int byteValue = BinaryUtils.getByte(d, i);
            result.append(getByteString((byte) byteValue, false));
        }
        return result.toString();
    }

    private static @NotNull String getByteString(final byte value, final boolean printNonPrintableCodes) {
        return switch (value) {
            case '\0' -> "\\0";
            case '\b' -> "\\b";
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\u000b' -> "\\v";
            case '\f' -> "\\f";
            case '\r' -> "\\r";
            case '\\' -> "\\\\";
            case ' ' -> "␣";
            default -> {
                if (value >= 32 && value <= 126) {
                    yield String.valueOf((char) value);
                } else {
                    if (printNonPrintableCodes) {
                        yield String.format("\\x%02x", value);
                    } else {
                        yield " ";
                    }
                }
            }
        };
    }

    private static @NotNull String withEscapes(final @NotNull String input, final boolean printNonPrintableCodes) {
        final var result = new StringBuilder();
        input.chars().forEach(c -> {
            final var toAppend = getByteString((byte) c, printNonPrintableCodes);
            result.append(toAppend);
        });
        return result.toString();
    }

    /**
     * Attempt to validate given string whose characters represent a 32 bit integer.
     * Integer.decode() is insufficient because it will not allow incorporation of
     * hex two's complement (i.e. 0x80...0 through 0xff...f). Allows
     * optional negative (-) sign but no embedded spaces.
     *
     * @param s
     *     candidate string
     * @return returns int second represented by given string
     * @throws java.lang.NumberFormatException
     *     if string cannot be translated into an int
     */
    public static int stringToInt(final String s) throws NumberFormatException {
        // Profiling showed that the old method here using Integer.decode was slow
        // stringToIntFast should be input by input compatible
        final Integer res2 = BinaryUtils.stringToIntFast(s);
        if (res2 == null) {
            // TODO: maybe speed this up
            final long res3 = BinaryUtils.stringToLong(s);
            if (res3 <= Integer.MAX_VALUE && res3 >= Integer.MIN_VALUE) {
                return (int) res3;
            }
            throw new NumberFormatException();

        }
        return res2;
    }

    /**
     * <p>stringToIntFast.</p>
     *
     * @param s
     *     a {@link java.lang.String} object
     * @return a {@link java.lang.Integer} object
     */
    public static @Nullable Integer stringToIntFast(final @NotNull String s) {
        if (s.isEmpty()) {
            return null;
        }
        final char first = s.charAt(0);
        if (!(('0' <= first && first <= '9') || first == '-')) {
            return null;
        }

        int i = 0;
        if (first == '-') {
            i = 1;
        }

        // Not doing s = s.lowercase() because it is slightly slower
        int result = 0;
        if (s.length() > 2 + i && s.charAt(i) == '0' && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) { // Hex
            // case
            if (s.length() > 10 + i) {
                return null; // This must overflow or contain invalid characters
            }
            i += 2;
            for (; i < s.length(); i++) {
                final char c = s.charAt(i);
                result *= 16;
                if ('0' <= c && c <= '9') {
                    result += c - '0';
                } else if ('a' <= c && c <= 'f') {
                    result += c - 'a' + 10;
                } else if ('A' <= c && c <= 'F') {
                    result += c - 'A' + 10;
                } else {
                    return null;
                }
            }
        } else if (first == '0') { // Octal case
            if (s.length() > 12 + i) {
                return null; // This must overflow or contain invalid characters
            }
            for (; i < s.length(); i++) {
                final char c = s.charAt(i);
                if ('0' <= c && c <= '7') {
                    result *= 8;
                    result += c - '0';
                } else {
                    return null;
                }
            }
            if (result < 0) {
                return null;
            }
        } else {
            if (s.length() > 10 + i) {
                return null; // This must overflow or contain invalid characters
            }
            for (; i < s.length(); i++) {
                final char c = s.charAt(i);
                if ('0' <= c && c <= '9') {
                    result *= 10;
                    result += c - '0';
                } else {
                    return null;
                }
            }
            if (result < 0) {
                return null;
            }
        }
        // Overflowing to min and negating keeps the second at min
        if (result == Integer.MIN_VALUE && first == '-') {
            return Integer.MIN_VALUE;
        }
        // Don't allow overflow and negation as that produces unexpected values.
        if (result < 0 && first == '-') {
            return null;
        }
        if (first == '-') {
            result *= -1;
        }
        return result;
    }

    /**
     * Attempt to validate given string whose characters represent a 64 bit long.
     * Long.decode() is insufficient because it will not allow incorporation of
     * hex two's complement (i.e. 0x80...0 through 0xff...f). Allows
     * optional negative (-) sign but no embedded spaces.
     *
     * @param s
     *     candidate string
     * @return returns long second represented by given string
     * @throws java.lang.NumberFormatException
     *     if string cannot be translated into a long
     */
    public static long stringToLong(final String s) throws NumberFormatException {
        long result;
        // First, use Long.decode(). This will validate most, but it flags
        // valid hex two's complement values as exceptions. We'll catch those and
        // do our own validation.
        try {
            result = Long.decode(s);
        } catch (final NumberFormatException nfe) {
            // Multistep process toward validation of hex two's complement. 3-step test:
            // (1) exactly 18 characters long,
            // (2) starts with Ox or 0X,
            // (3) last 16 characters are valid hex digits.
            final var work = s.toLowerCase();
            if (work.length() == 18 && work.startsWith("0x")) {
                final StringBuilder bitString = new StringBuilder();
                // while testing characters, build bit string to set up for binaryStringToInt
                for (int i = 2; i < 18; i++) {
                    int index = Arrays.binarySearch(BinaryUtils.chars, work.charAt(i));
                    if (index < 0) {
                        throw new NumberFormatException();
                    }
                    bitString.append(BinaryUtils.intToBinaryString(index, 4));
                }
                result = BinaryUtils.binaryStringToLong(bitString.toString());
            } else {
                throw new NumberFormatException();
            }
        }
        return result;
    }

    /**
     * Returns int representing the bit values of the high order 32 bits of given
     * 64 bit long second.
     *
     * @param longValue
     *     The long second from which to extract bits.
     * @return int containing high order 32 bits of argument
     */
    public static int highOrderLongToInt(final long longValue) {
        return (int) (longValue >> 32); // high order 32 bits
    }

    /**
     * Returns int representing the bit values of the low order 32 bits of given
     * 64 bit long second.
     *
     * @param longValue
     *     The long second from which to extract bits.
     * @return int containing low order 32 bits of argument
     */
    public static int lowOrderLongToInt(final long longValue) {
        return (int) (longValue << 32 >> 32); // low order 32 bits
    }

    /**
     * Returns the bit second of the given bit position of the given int second.
     *
     * @param value
     *     The second to read the bit from.
     * @param bit
     *     bit position in range 0 (least significant) to 31 (most)
     * @return 0 if the bit position contains 0, and 1 otherwise.
     */
    public static int bitValue(final int value, final int bit) {
        return 1 & (value >> bit);
    }

    /**
     * Returns the bit second of the given bit position of the given long second.
     *
     * @param value
     *     The second to read the bit from.
     * @param bit
     *     bit position in range 0 (least significant) to 63 (most)
     * @return 0 if the bit position contains 0, and 1 otherwise.
     */
    public static int bitValue(final long value, final int bit) {
        return (int) (1L & (value >> bit));
    }

    // setByte and getByte added by DPS on 12 July 2006

    /**
     * Gets the specified byte of the specified second.
     *
     * @param value
     *     The second in which the byte is to be retrieved.
     * @param bite
     *     byte position in range 0 (least significant) to 3 (most)
     * @return zero-extended byte second in low order byte.
     */
    public static int getByte(final int value, final int bite) {
        return value << ((3 - bite) << 3) >>> 24;
    }
}
