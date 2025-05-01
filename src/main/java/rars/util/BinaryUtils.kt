@file:JvmName("BinaryUtilsKt")

package rars.util

import rars.api.DisplayFormat
import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat

@JvmName("intToBinaryString")
fun Int.toBinaryString(length: Int): String = toUInt().toString(radix = 2).padStart(32, '0').let {
    it.substring(it.length - length)
}

/**
 * Prefix a hexadecimal-indicating string "0x" to the string which is
 * returned by the method "Integer.toHexString". Prepend leading zeroes
 * to that string as necessary to make it always eight hexadecimal digits.
 *
 * @return String containing '0', '1', ... 'F' which form hexadecimal equivalent
 * of int.
 */
@JvmName("intToHexStringWithPrefix")
fun Int.toHexStringWithPrefix(): String = buildString {
    val base = Integer.toHexString(this@toHexStringWithPrefix)
    append("0x")
    repeat(8 - base.length) {
        append('0')
    }
    append(base)
}

/**
 * Prefix a hexadecimal-indicating string "0x" to the string equivalent to the
 * hexadecimal value in the long parameter. Prepend leading zeroes
 * to that string as necessary to make it always sixteen hexadecimal digits.
 *
 * @param value
 *     The long value to convert.
 * @return String containing '0', '1', ...'F' which form hexadecimal equivalent
 * of long.
 */
@JvmName("longToHexStringWithPrefix")
fun Long.toHexStringWithPrefix(): String = buildString {
    val base = java.lang.Long.toHexString(this@toHexStringWithPrefix)
    append("0x")
    repeat(16 - base.length) {
        append('0')
    }
    append(base)
}


@JvmName("longToBinaryString")
fun Long.toBinaryString(): String = toBinaryString(Long.SIZE_BITS)

@JvmName("longToBinaryString")
fun Long.toBinaryString(length: Int): String = buildString {
    for (i in (length - 1) downTo 0) {
        // Shift this Long right by i bits and mask out all but the lowest order bit.
        append(if ((this@toBinaryString shr i) and 1 == 1L) '1' else '0')
    }
}

fun Long.toDoubleReinterpreted(): Double = longBitsToDouble(this)
fun Double.toLongReinterpreted(): Long = doubleToRawLongBits(this)

fun Float.toIntReinterpreted(): Int = floatToIntBits(this)
fun Int.toFloatReinterpreted(): Float = intBitsToFloat(this)

/**
 * Attempt to validate given string whose characters represent a 64 bit long.
 * Long.decode() is insufficient because it will not allow incorporation of
 * hex two's complement (i.e. 0x80...0 through 0xff...f). Allows
 * optional negative (-) sign but no embedded spaces.
 *
 * @param s
 *     candidate string
 * @return returns long value represented by given string
 * @throws java.lang.NumberFormatException
 *     if string cannot be translated into a long
 */
@JvmName("stringToLong")
fun String.translateToLong(): Long? = try {
    java.lang.Long.decode(this)
} catch (_: NumberFormatException) {
    // If Long.decode fails, check if it looks like a hex two's complement value.
    // We expect exactly 18 characters (i.e. "0x" followed by 16 hex digits).
    if (this.length == 18 && this.startsWith("0x", ignoreCase = true)) {
        // Remove the "0x" prefix and attempt to convert the remaining hex string.
        // toULongOrNull returns null if the conversion fails.
        this.substring(2).toULongOrNull(16)?.toLong()
    } else null
}

/**
 * Attempt to validate given string whose characters represent a 32-bit integer.
 * Integer.decode() is insufficient because it will not allow incorporation of
 * hex two's complement (i.e. 0x80...0 through 0xff...f). Allows
 * optional negative (-) sign but no embedded spaces.
 *
 * @return returns int value represented by given string or null if string is not
 * a valid representation of a 32-bit integer.
 */
@JvmName("stringToInt")
fun String.translateToInt(): Int? = translateToIntFast() ?: translateToLong()
    ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
    ?.toInt()

@JvmName("stringToIntFast")
fun String.translateToIntFast(): Int? {
    if (this.isEmpty()) return null
    val first = this.first()
    if (first !in '0'..'9' && first != '-') return null

    var index = if (first == '-') 1 else 0
    var result = 0

    when {
        // Hex case
        this.length > 2 + index && this[index] == '0' && (this[index + 1] == 'x' || this[index + 1] == 'X') -> {
            if (this.length > 10 + index) {
                return null // This must overflow or contain invalid characters
            }
            index += 2
            while (index < this.length) {
                val c: Char = this[index]
                result *= 16
                result += when (c) {
                    in '0'..'9' -> c.code - '0'.code
                    in 'a'..'f' -> c.code - 'a'.code + 10
                    in 'A'..'F' -> c.code - 'A'.code + 10
                    else -> return null
                }
                index++
            }
        }
        // Octal case
        first == '0' -> {
            if (this.length > 12) {
                return null // This must overflow or contain invalid characters
            }
            while (index < this.length) {
                val c: Char = this[index]
                if (c in '0'..'7') {
                    result *= 8
                    result += c.code - '0'.code
                } else return null
                index++
            }
            if (result < 0) {
                return null
            }
        }
        else -> {
            if (this.length > 10 + index) {
                return null // This must overflow or contain invalid characters
            }
            while (index < this.length) {
                val chr = this[index]
                if (chr in '0'..'9') {
                    result *= 10
                    result += chr.code - '0'.code
                } else {
                    return null
                }
                index++
            }
            if (result < 0) {
                return null
            }
        }
    }

    // Overflowing to min and negating keeps the value at min
    if (result == Int.MIN_VALUE && first == '-') return Int.MIN_VALUE

    // Don't allow overflow and negation as that produces unexpected values.
    if (result < 0 && first == '-') return null

    if (first == '-') result *= -1

    return result
}

@JvmName("stringWithEscapes")
fun String.withEscapes(printNonPrintableCodes: Boolean = true): String = buildString {
    this@withEscapes.toByteArray().forEach {
        append(it.toEscapedString())
    }
}

fun Byte.toEscapedString(): String = when (this) {
    '\u0000'.code.toByte() -> "\\0"
    '\b'.code.toByte() -> "\\b"
    '\t'.code.toByte() -> "\\t"
    '\n'.code.toByte() -> "\\n"
    '\u000b'.code.toByte() -> "\\v"
    '\u000c'.code.toByte() -> "\\f"
    '\r'.code.toByte() -> "\\r"
    '\\'.code.toByte() -> "\\\\"
    ' '.code.toByte() -> "␣"
    else -> {
        if (this in 32..126) {
            this.toInt().toChar().toString()
        } else {
            "\\x%02x".format(this)
        }
    }
}

/**
 * Produce ASCII string equivalent of integer value, interpreting it as 4
 * one-byte
 * characters. If the value in a given byte does not correspond to a printable
 * character, it will be assigned a default character for a placeholder.
 *
 * @return String that represents ASCII equivalent
 */
@JvmName("intToAscii")
fun Int.toAscii(): String = buildString(capacity = 4) {
    bytes.forEach { byte ->
        append(byte.toEscapedString())
    }
}

@JvmName("longToAscii")
fun Long.toAscii(): String = buildString(capacity = 16) {
    bytes.forEach { byte ->
        append(byte.toEscapedString())
    }
}

val Int.bytes
    get() = ByteArray(4) {
        (this@bytes shr (24 - it * 8)).toByte()
    }

val Long.bytes
    get() = ByteArray(8) {
        (this shr (56 - it * 8)).toByte()
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
fun hexStringToBinaryString(value: String): String {
    val trimmed = if (value.startsWith("0x", ignoreCase = true)) {
        value.substring(2)
    } else {
        value
    }
    return buildString(capacity = trimmed.length * 4) {
        for (chr in trimmed) {
            val toAppend = when (chr) {
                '0' -> "0000"
                '1' -> "0001"
                '2' -> "0010"
                '3' -> "0011"
                '4' -> "0100"
                '5' -> "0101"
                '6' -> "0110"
                '7' -> "0111"
                '8' -> "1000"
                '9' -> "1001"
                'a', 'A' -> "1010"
                'b', 'B' -> "1011"
                'c', 'C' -> "1100"
                'd', 'D' -> "1101"
                'e', 'E' -> "1110"
                'f', 'F' -> "1111"
                else -> error("Invalid hex character: $chr")
            }
            append(toAppend)
        }
    }
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
 * @return String containing '0', '1', ..., 'F' characters which form hexadecimal
 * equivalent of decoded String.
 */
fun binaryStringToHexString(value: String): String {
    val digits = (value.length + 3) / 4
    val hexChars = CharArray(digits + 2)
    hexChars[0] = '0'
    hexChars[1] = 'x'
    var position = value.length - 1
    for (digit in 0..<digits) {
        var result = 0
        var pow = 1
        var rep = 0
        while (rep < 4 && position >= 0) {
            if (value[position] == '1') {
                result += pow
            }
            pow *= 2
            position--
            rep++
        }
        hexChars[digits - digit + 1] = chars[result]
    }
    return String(hexChars)
}

fun Int.bitValue(bit: Int): Int {
    require(bit in 0..<Int.SIZE_BITS) { "Bit index out of range: $bit" }
    return (this shr bit) and 1
}

/**
 * Returns int representing the bit values of the high order 32 bits of given
 * 64 bit long value.
 *
 * @return int containing high order 32 bits of argument
 */
fun Long.upperToInt(): Int = (this ushr Int.SIZE_BITS).toInt()

private val chars = charArrayOf(
    '0', '1', '2', '3',
    '4', '5', '6', '7',
    '8', '9', 'a', 'b',
    'c', 'd', 'e', 'f'
)

fun unsignedIntToIntString(value: Int): String = value.toUInt().toString()

/**
 * Produces a string form of a float given an integer containing
 * the 32 bit pattern and the numerical base to use (10 or 16). If the
 * base is 16, the string will be built from the 32 bits. If the
 * base is 10, the int bits will be converted to float and the
 * string constructed from that. Seems an odd distinction to make,
 * except that contents of floating point registers are stored
 * internally as int bits. If the int bits represent a NaN value
 * (of which there are many!), converting them to float then calling
 * formatNumber(float, int) above, causes the float value to become
 * the canonical NaN value 0x7fc00000. It does not preserve the bit
 * pattern! Then converting it to hex string yields the canonical NaN.
 * Not an issue if display base is 10 since result string will be NaN
 * no matter what the internal NaN value is.
 *
 * @param value
 *     the int bits to be converted to string of corresponding float.
 * @param format
 *     the format to use
 * @return a String equivalent of the value rendered appropriately.
 */
fun formatFloatNumber(value: Int, format: DisplayFormat): String {
    return when (format) {
        DisplayFormat.DECIMAL -> value.toFloatReinterpreted().toString()
        DisplayFormat.HEX -> value.toHexStringWithPrefix()
        DisplayFormat.ASCII -> value.toAscii()
    }
}

fun Float.formatToString(format: DisplayFormat): String {
    return when (format) {
        DisplayFormat.DECIMAL -> toString()
        DisplayFormat.HEX -> toInt().toHexStringWithPrefix()
        DisplayFormat.ASCII -> toInt().toAscii()
    }
}

@JvmName("intFormatToString")
fun Int.formatToString(format: DisplayFormat): String = when (format) {
    DisplayFormat.DECIMAL -> toString()
    DisplayFormat.HEX -> toHexStringWithPrefix()
    DisplayFormat.ASCII -> toAscii()
}