package rars.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KClass

private fun Level.toRARSLogLevel(): LogLevel = when (this) {
    Level.TRACE -> LogLevel.TRACE
    Level.DEBUG -> LogLevel.DEBUG
    Level.INFO -> LogLevel.INFO
    Level.WARN -> LogLevel.WARNING
    Level.ERROR -> LogLevel.ERROR
    Level.FATAL -> LogLevel.FATAL
    Level.OFF -> LogLevel.NONE
    else -> error("Unknown log level: $this")
}

private fun LogLevel.toLog4jLevel(): Level = when (this) {
    LogLevel.NONE -> Level.OFF
    LogLevel.FATAL -> Level.FATAL
    LogLevel.ERROR -> Level.ERROR
    LogLevel.WARNING -> Level.WARN
    LogLevel.INFO -> Level.INFO
    LogLevel.DEBUG -> Level.DEBUG
    LogLevel.TRACE -> Level.TRACE
}

internal data object Log4jLoggerFactory : RARSLoggerFactory {
    override fun forClass(cls: KClass<*>): RARSLogger {
        cls.java.kotlin
        val baseLogger = LogManager.getLogger(cls.java)
        return Log4jLogger(baseLogger)
    }

    override fun forName(name: String): RARSLogger {
        val baseLogger = LogManager.getLogger(name)
        return Log4jLogger(baseLogger)
    }
}

private data class Log4jLogger(
    private val baseLogger: Logger
) : RARSLogger {
    override val logLevel: LogLevel
        get() = baseLogger.level.toRARSLogLevel()

    override fun log(level: LogLevel, message: Any?) {
        baseLogger.log(level.toLog4jLevel(), message)
    }

    override fun log(
        level: LogLevel,
        lazyMessage: () -> Any?
    ) {
        baseLogger.log(level.toLog4jLevel(), lazyMessage)
    }

    override fun log(
        level: LogLevel,
        exception: Throwable,
        message: Any?
    ) {
        baseLogger.log(level.toLog4jLevel(), message, exception)
    }

    override fun log(
        level: LogLevel,
        exception: Throwable,
        lazyMessage: () -> Any?
    ) {
        baseLogger.log(level.toLog4jLevel(), lazyMessage, exception)
    }
}