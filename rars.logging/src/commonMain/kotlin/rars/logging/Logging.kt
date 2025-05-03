package rars.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.reflect.KClass

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

interface Logger {
    val logLevel: LogLevel

    fun log(
        level: LogLevel,
        exception: Throwable? = null,
        lazyMessage: () -> Any?
    )
}

interface LoggerFactory {
    fun forClass(cls: KClass<*>): Logger
    fun forName(name: String): Logger
    fun forObject(obj: Any): Logger = forClass(obj::class)

    companion object {
        fun create(builderFunc: LoggerFactoryBuilder.() -> Unit): LoggerFactory =
            LoggerFactoryBuilderImpl().apply(builderFunc).build()

        fun noopFactory(): LoggerFactory = NoopLoggerFactory
        fun noopLogger(): Logger = NoopLogger
    }
}

interface LoggerFactoryBuilder {
    var logLevel: LogLevel
    fun logFormat(format: LoggingFormat)
    fun appender(appender: Appender)
}

private class LoggerFactoryBuilderImpl() : LoggerFactoryBuilder {
    private var format: LoggingFormat? = null
    private var _logLevel: LogLevel? = null
    private val appenders = mutableSetOf<Appender>()

    override var logLevel: LogLevel
        get() = _logLevel ?: error {
            "Log level not set before reading it's value."
        }
        set(value) {
            _logLevel = value
        }

    override fun logFormat(format: LoggingFormat) {
        this.format = format
    }

    override fun appender(appender: Appender) {
        appenders.add(appender)
    }

    fun build(): LoggerFactory {
        requireNotNull(format) {
            """
            Logging format must be set when creating a logger factory.
            Make sure to call the logFormat() method in the builder.
            """.trimIndent()
        }
        requireNotNull(_logLevel) {
            """
            Log level must be set before creating a logger factory.
            Make sure to assign a LogLevel value to the logLevel variable.
            """.trimIndent()
        }
        return if (_logLevel == LogLevel.NONE || appenders.isEmpty()) {
            NoopLoggerFactory
        } else {
            LoggerFactoryImpl(format!!, _logLevel!!, appenders)
        }
    }
}

private class LoggerFactoryImpl(
    private val format: LoggingFormat,
    private val logLevel: LogLevel,
    private val appenders: Set<Appender>,
) : LoggerFactory {
    override fun forClass(cls: KClass<*>): Logger = LoggerImpl(
        LoggerName.ClassName(cls),
        logLevel,
        format,
        appenders
    )

    override fun forName(name: String): Logger = LoggerImpl(
        LoggerName.StringName(name),
        logLevel,
        format,
        appenders
    )
}

private class LoggerImpl(
    private val name: LoggerName,
    override val logLevel: LogLevel,
    format: LoggingFormat,
    appenders: Set<Appender>,
) : LoggerBase(format, appenders) {
    override fun collectContext(
        level: LogLevel,
        message: Any?,
        throwable: Throwable?
    ): LogContext = ContextImpl(
        timestamp = Clock.System.now(),
        loggerName = name,
        message = message,
        exception = throwable,
        level = level,
    )
}

private data class ContextImpl(
    override val timestamp: Instant,
    override val loggerName: LoggerName,
    override val message: Any?,
    override val exception: Throwable?,
    override val level: LogLevel
) : LogContext

fun interface LoggingFormat {
    fun LogContext.format(): String
}

interface LogContext {
    val timestamp: Instant
    val loggerName: LoggerName
    val message: Any?
    val exception: Throwable?
    val level: LogLevel
}

/**
 * The [Appender] interface specifies the destination for log messages.
 */
fun interface Appender {
    fun append(message: String, logLevel: LogLevel)
}

sealed interface LoggerName {
    data class ClassName(val cls: KClass<*>) : LoggerName
    data class StringName(val name: String) : LoggerName
}