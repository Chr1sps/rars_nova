package rars.logging


private fun LogLevel.isEnabledFor(level: LogLevel): Boolean =
    this != LogLevel.NONE &&
        level != LogLevel.NONE &&
        this <= level

abstract class LoggerBase(
    private val format: LoggingFormat,
    private val appenders: Set<Appender>,
) : Logger {

    final override fun log(
        level: LogLevel,
        exception: Throwable?,
        lazyMessage: () -> Any?
    ) {
        if (level.isEnabledFor(logLevel)) {
            val context = collectContext(level, lazyMessage(), exception)
            val message = format.run {
                context.format()
            }
            doLog(message, logLevel)
        }
    }

    protected abstract fun collectContext(
        level: LogLevel,
        message: Any?,
        throwable: Throwable? = null
    ): LogContext


    private fun doLog(message: String, logLevel: LogLevel) {
        appenders.forEach { it.append(message, logLevel) }
    }
}