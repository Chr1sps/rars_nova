package rars.logging

import kotlin.reflect.KClass

internal data object NoopLoggerFactory : LoggerFactory {
    override fun forClass(cls: KClass<*>): Logger = NoopLogger
    override fun forName(name: String): Logger = NoopLogger
    override fun forObject(obj: Any): Logger = NoopLogger
}

internal data object NoopLogger : Logger {
    override val logLevel = LogLevel.NONE

    override fun log(
        level: LogLevel,
        exception: Throwable?,
        lazyMessage: () -> Any?
    ) = Unit
}
