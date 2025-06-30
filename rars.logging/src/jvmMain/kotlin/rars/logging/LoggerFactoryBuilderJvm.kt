package rars.logging


internal class LoggerFactoryBuilderJvm : AbstractLoggerFactoryBuilder() {
    override fun loggerFactory(
        logLevel: LogLevel,
        appenders: Set<Appender>
    ): LoggerFactory = LoggerFactoryJvm(logLevel, appenders)
}