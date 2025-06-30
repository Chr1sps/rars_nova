package rars.runtime

data class MemoryError<T>(
    val message: String,
    val reason: ExceptionReason.Memory,
    val address: T,
)