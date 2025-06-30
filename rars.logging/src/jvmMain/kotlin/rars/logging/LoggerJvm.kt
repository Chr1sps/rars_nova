package rars.logging

internal class LoggerJvm(
    name: LoggerName,
    logLevel: LogLevel,
    appenders: Set<Appender>,
) : AbstractLogger(name, logLevel, appenders) {
    override fun collectContext(
        level: LogLevel,
        exception: Throwable?,
        message: Any?
    ): LogContext = LogContextJvm.create(
        name,
        message,
        exception,
        level,
    )
}