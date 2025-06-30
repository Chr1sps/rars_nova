package rars.util

import kotlin.experimental.and

class ShortBitSet internal constructor(
    private val data: ShortArray,
    val bitsCount: Int,
) {
    constructor(bitsCount: Int)
        : this(ShortArray(arraySizeFor(bitsCount)), bitsCount)

    operator fun get(bitIndex: Int): Boolean =
        data[indexFor(bitIndex)] and
            (1 shl shiftFor(bitIndex)).toShort() !=
            0.toShort()

    val shortCount: Int get() = data.size
    fun toShortArray(): ShortArray = data.copyOf()

    companion object {
        private fun arraySizeFor(bitsCount: Int) =
            (bitsCount + Short.SIZE_BITS - 1) / Short.SIZE_BITS

        private fun indexFor(bitIndex: Int) = bitIndex / Short.SIZE_BITS
        private fun shiftFor(bitIndex: Int) = bitIndex % Short.SIZE_BITS

        private fun Int.roundUpToMultipleOf(base: Int): Int =
            (this + base - 1) / base * base
    }
}


fun Int.toShortBitSet(bitCount: Int): ShortBitSet {
    val array = if (bitCount > Short.SIZE_BITS) shortArrayOf(
        this.toShort(),
        this.shr(Short.SIZE_BITS).toShort(),
    ) else shortArrayOf(this.toShort())
    return ShortBitSet(array, bitCount)
}