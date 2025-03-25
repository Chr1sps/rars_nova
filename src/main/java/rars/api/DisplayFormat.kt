package rars.api


enum class DisplayFormat {
    DECIMAL,
    HEX,
    ASCII;

    override fun toString(): String = name.lowercase()
}
