package rars.notices

enum class AccessType(@JvmField val repr: String) {
    READ("Read"),
    WRITE("Write")
}