@file:JvmName("BinaryUtilsKt")

package rars.util

import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat

/**
 * Translate int value into a String consisting of '1's and '0's. Assumes all 32
 * bits are
 * to be translated.
 *
 * @return String consisting of '1' and '0' characters corresponding to the
 * requested binary sequence.
 */
@JvmName("intToBinaryString")
fun Int.toBinaryString(): String = toBinaryString(Int.SIZE_BITS)

@JvmName("intToBinaryString")
fun Int.toBinaryString(length: Int): String = buildString {
    for (i in (length - 1) downTo 0) {
        // Shift this Int right by i bits and mask out all but the lowest order bit.
        append(if ((this@toBinaryString shr i) and 1 == 1) '1' else '0')
    }
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

fun Byte.toEscapedString(): String {
    return when (this) {
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
                if (true) {
                    "\\x%02x".format(this)
                } else {
                    " "
                }
            }
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
fun Int.toAscii(): String {
    val result = StringBuilder(8)
    this.bytes.forEach {
        result.append(it.toEscapedString())
    }
    return result.toString()
}

val Int.bytes: ByteArray
    get() = byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )