package rars.logging

internal abstract class AbstractLogger(
    protected val name: LoggerName,
    override val logLevel: LogLevel,
    protected val appenders: Set<Appender>,
) : Logger {
    final override fun log(
        level: LogLevel,
        exception: Throwable?,
        lazyMessage: () -> Any?
    ) {
        if (level.isEnabledFor(logLevel)) {
            val context = collectContext(
                level,
                exception,
                lazyMessage(),
            )
            appenders.forEach { it.append(context) }
        }
    }

    abstract fun collectContext(
        level: LogLevel,
        exception: Throwable?,
        message: Any?,
    ): LogContext
}