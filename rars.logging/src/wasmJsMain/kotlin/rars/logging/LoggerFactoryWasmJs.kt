package rars.logging

import kotlin.reflect.KClass

actual interface LoggerFactory {
    actual fun forClass(cls: KClass<*>): Logger
    actual fun forName(name: String): Logger

    actual companion object {
        actual fun create(builderFunc: LoggerFactoryBuilder.() -> Unit): LoggerFactory =
            LoggerFactoryBuilderWasmJs().apply(builderFunc).build()
    }

}

internal class LoggerFactoryWasmJs(
    private val logLevel: LogLevel,
    private val appenders: Set<Appender>
) : LoggerFactory {
    override fun forClass(cls: KClass<*>): Logger = LoggerWasmJs(
        LoggerName.Class(cls),
        logLevel,
        appenders,
    )

    override fun forName(name: String): Logger = LoggerWasmJs(
        LoggerName.String(name),
        logLevel,
        appenders,
    )
}