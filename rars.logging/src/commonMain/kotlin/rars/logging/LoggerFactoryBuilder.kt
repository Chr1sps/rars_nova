package rars.logging

interface LoggerFactoryBuilder {
    var logLevel: LogLevel
    fun appender(appender: Appender)
}

fun LoggerFactoryBuilder.formattedAppender(
    format: LoggingFormat,
    outputFunc: (String) -> Unit,
) {
    appender { context ->
        val formattedMessage = format.run { context.format() }
        outputFunc(formattedMessage)
    }
}

internal abstract class AbstractLoggerFactoryBuilder : LoggerFactoryBuilder {
    private var _logLevel: LogLevel? = null
    private val appenders = mutableSetOf<Appender>()

    final override var logLevel: LogLevel
        get() = _logLevel ?: error {
            "Log level not set before reading it's value."
        }
        set(value) {
            _logLevel = value
        }

    final override fun appender(appender: Appender) {
        appenders.add(appender)
    }

    fun build(): LoggerFactory {
        requireNotNull(_logLevel) {
            """
            Log level must be set before creating a logger factory.
            Make sure to assign a LogLevel value to the logLevel variable.
            """.trimIndent()
        }
        return if (_logLevel == LogLevel.NONE || appenders.isEmpty()) {
            NoopLoggerFactory
        } else {
            loggerFactory(_logLevel!!, appenders)
        }
    }

    abstract fun loggerFactory(
        logLevel: LogLevel,
        appenders: Set<Appender>
    ): LoggerFactory
}
