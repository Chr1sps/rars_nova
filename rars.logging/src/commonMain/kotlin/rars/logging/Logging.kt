package rars.logging

import kotlinx.datetime.Instant

enum class LogLevel {
    NONE,
    FATAL,
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    TRACE;

    override fun toString(): String = name.uppercase()
}

/**
 * Interface defining the logging functionality.
 *
 * The Logger interface provides methods to log messages with different severity levels.
 * It supports logging messages with optional exceptions and lazy message evaluation.
 */
interface Logger {

    /**
     * The logging level of this logger.
     *
     * This field is assumed to have a constant value for a given logger instance.
     */
    val logLevel: LogLevel

    /**
     * Records a log message with specified severity level and optional exception.
     * The message is evaluated lazily only if the message's severity level
     * is enabled for this logger.
     * @param level the severity level of the log message
     * @param exception the exception to be logged (optional)
     * @param lazyMessage function that produces the log message
     */
    fun log(
        level: LogLevel,
        exception: Throwable? = null,
        lazyMessage: () -> Any?
    )
}

/**
 * Stores all the information about a given logging event.
 */
expect interface LogContext {
    val timestamp: Instant
    val loggerName: LoggerName
    val message: Any?
    val exception: Throwable?
    val level: LogLevel
}

/**
 * The [Appender] interface specifies the destination for log messages
 * (console, file, etc.).
 */
fun interface Appender {
    fun append(context: LogContext)
}

/**
 * Specifies the name of the logger either via a reference to its class or
 * to its custom name. Platform implementations (i.e., JVM) may use
 * platform-specific class types for the class references.
 */
expect sealed interface LoggerName 