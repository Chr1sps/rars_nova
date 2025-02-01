package rars.util

/**
 * Translate int value into a String consisting of '1's and '0's. Assumes all 32
 * bits are
 * to be translated.
 *
 * @param value
 *     The int value to convert.
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
 * @param d
 *     The int value to convert.
 * @return String containing '0', '1', ...'F' which form hexadecimal equivalent
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


@JvmName("longToBinaryString")
fun Long.toBinaryString(): String = toBinaryString(Long.SIZE_BITS)

@JvmName("longToBinaryString")
fun Long.toBinaryString(length: Int): String = buildString {
    for (i in (length - 1) downTo 0) {
        // Shift this Long right by i bits and mask out all but the lowest order bit.
        append(if ((this@toBinaryString shr i) and 1 == 1L) '1' else '0')
    }
}