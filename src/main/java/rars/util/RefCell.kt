package rars.util

class RefCell<T>(@JvmField var value: T)
class IntRefCell(@JvmField var value: Int)
class BooleanRefCell(@JvmField var value: Boolean)

fun <T> T.toRefCell() = RefCell(this)

