package rars.logging

import kotlin.reflect.KClass

internal expect object NoopLoggerFactory : LoggerFactory {
    override fun forClass(cls: KClass<*>): Logger
    override fun forName(name: String): Logger
}

internal data object NoopLogger : Logger {
    override val logLevel = LogLevel.NONE

    override fun log(
        level: LogLevel,
        exception: Throwable?,
        lazyMessage: () -> Any?
    ) = Unit
}
