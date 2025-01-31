package rars.api

import java.util.*

enum class DisplayFormat {
    DECIMAL,
    HEX,
    ASCII;

    override fun toString(): String = name.lowercase(Locale.getDefault())
}
