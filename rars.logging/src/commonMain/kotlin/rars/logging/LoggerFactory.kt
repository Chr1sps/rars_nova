package rars.logging

import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * A logger factory provides methods to create class-specific (or named) loggers.
 */
expect interface LoggerFactory {
    fun forClass(cls: KClass<*>): Logger
    fun forName(name: String): Logger

    companion object {
        @JvmStatic
        fun create(builderFunc: LoggerFactoryBuilder.() -> Unit): LoggerFactory
    }
}

fun LoggerFactory.forObject(obj: Any): Logger = forClass(obj::class)
fun LoggerFactory.Companion.noopFactory(): LoggerFactory = NoopLoggerFactory
fun LoggerFactory.Companion.noopLogger(): Logger = NoopLogger

fun interface LoggingFormat {
    fun LogContext.format(): String
}

