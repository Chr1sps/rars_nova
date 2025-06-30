package rars.logging

internal class LoggerWasmJs(
    name: LoggerName,
    logLevel: LogLevel,
    appenders: Set<Appender>
) : AbstractLogger(name, logLevel, appenders) {
    override fun collectContext(
        level: LogLevel,
        exception: Throwable?,
        message: Any?
    ): LogContext = LogContextWasmJs.create(
        name,
        message,
        exception,
        level,
    )
}