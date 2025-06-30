package rars.logging

import kotlin.reflect.KClass

actual interface LoggerFactory {
    actual fun forClass(cls: KClass<*>): Logger
    actual fun forName(name: String): Logger
    fun forJavaClass(cls: Class<*>): Logger

    actual companion object {
        @JvmStatic
        actual fun create(builderFunc: LoggerFactoryBuilder.() -> Unit): LoggerFactory =
            LoggerFactoryBuilderJvm().apply(builderFunc).build()
    }
}

internal class LoggerFactoryJvm(
    private val logLevel: LogLevel,
    private val appenders: Set<Appender>
) : LoggerFactory {
    override fun forClass(cls: KClass<*>): Logger = forJavaClass(cls.java)

    override fun forName(name: String): Logger = LoggerJvm(
        LoggerName.String(name),
        logLevel,
        appenders,
    )

    override fun forJavaClass(cls: Class<*>): Logger = LoggerJvm(
        LoggerName.Class(cls),
        logLevel,
        appenders,
    )
}