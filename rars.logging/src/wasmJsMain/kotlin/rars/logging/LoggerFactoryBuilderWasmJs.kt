package rars.logging

internal class LoggerFactoryBuilderWasmJs : AbstractLoggerFactoryBuilder() {
    override fun loggerFactory(
        logLevel: LogLevel,
        appenders: Set<Appender>
    ): LoggerFactory = LoggerFactoryWasmJs(logLevel, appenders)
}
