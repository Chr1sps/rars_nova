package rars.logging

actual sealed interface LoggerName {
    data class Class(val cls: java.lang.Class<*>) : LoggerName
    data class String(val name: kotlin.String) : LoggerName
}
