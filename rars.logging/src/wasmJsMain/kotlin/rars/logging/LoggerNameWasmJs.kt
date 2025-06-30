package rars.logging

import kotlin.reflect.KClass

actual sealed interface LoggerName {
    data class String(val name: kotlin.String) : LoggerName
    data class Class(val cls: KClass<*>) : LoggerName
}