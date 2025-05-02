package rars.logging

import kotlin.reflect.KClass

enum class LogLevel {
    NONE,
    FATAL,
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    TRACE,
}

interface RARSLogger {
    val logLevel: LogLevel

    fun log(level: LogLevel, message: Any?)
    fun log(level: LogLevel, lazyMessage: () -> Any?)
    fun log(level: LogLevel, exception: Throwable, message: Any? = "")
    fun log(level: LogLevel, exception: Throwable, lazyMessage: () -> Any?)
}

fun RARSLogger.fatal(message: Any?) = log(LogLevel.FATAL, message)
fun RARSLogger.error(message: Any?) = log(LogLevel.ERROR, message)
fun RARSLogger.warning(message: Any?) = log(LogLevel.WARNING, message)
fun RARSLogger.info(message: Any?) = log(LogLevel.INFO, message)
fun RARSLogger.debug(message: Any?) = log(LogLevel.DEBUG, message)
fun RARSLogger.trace(message: Any?) = log(LogLevel.TRACE, message)

fun RARSLogger.fatal(lazyMessage: () -> Any?) = log(
    LogLevel.FATAL, lazyMessage
)

fun RARSLogger.error(lazyMessage: () -> Any?) = log(
    LogLevel.ERROR, lazyMessage
)

fun RARSLogger.warning(lazyMessage: () -> Any?) = log(
    LogLevel.WARNING, lazyMessage
)

fun RARSLogger.info(lazyMessage: () -> Any?) = log(
    LogLevel.INFO, lazyMessage
)

fun RARSLogger.debug(lazyMessage: () -> Any?) = log(
    LogLevel.DEBUG, lazyMessage
)

fun RARSLogger.trace(lazyMessage: () -> Any?) = log(
    LogLevel.TRACE, lazyMessage
)

fun RARSLogger.fatal(exception: Throwable, message: Any? = "") =
    log(LogLevel.FATAL, exception, message)

fun RARSLogger.error(exception: Throwable, message: Any? = "") =
    log(LogLevel.ERROR, exception, message)

fun RARSLogger.warning(exception: Throwable, message: Any? = "") =
    log(LogLevel.WARNING, exception, message)

fun RARSLogger.info(exception: Throwable, message: Any? = "") =
    log(LogLevel.INFO, exception, message)

fun RARSLogger.debug(exception: Throwable, message: Any? = "") =
    log(LogLevel.DEBUG, exception, message)

fun RARSLogger.trace(exception: Throwable, message: Any? = "") =
    log(LogLevel.TRACE, exception, message)

fun RARSLogger.fatal(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.FATAL, exception, lazyMessage)

fun RARSLogger.error(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.ERROR, exception, lazyMessage)

fun RARSLogger.warning(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.WARNING, exception, lazyMessage)

fun RARSLogger.info(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.INFO, exception, lazyMessage)

fun RARSLogger.debug(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.DEBUG, exception, lazyMessage)

fun RARSLogger.trace(exception: Throwable, lazyMessage: () -> Any?) =
    log(LogLevel.TRACE, exception, lazyMessage)

private data object NoopLogger : RARSLogger {
    override val logLevel: LogLevel = LogLevel.NONE

    override fun log(level: LogLevel, message: Any?) = Unit
    override fun log(level: LogLevel, lazyMessage: () -> Any?) = Unit
    override fun log(
        level: LogLevel, exception: Throwable, message: Any?
    ) = Unit

    override fun log(
        level: LogLevel, exception: Throwable, lazyMessage: () -> Any?
    ) = Unit
}


interface RARSLoggerFactory {
    fun forClass(cls: KClass<*>): RARSLogger
    fun forName(name: String): RARSLogger
    fun forObject(obj: Any): RARSLogger = forClass(obj::class)
}

object RARSLogging : RARSLoggerFactory by Log4jLoggerFactory {
    @JvmStatic
    fun forJavaClass(cls: Class<*>): RARSLogger = forClass(cls.kotlin)
}

data object NoopLoggerFactory : RARSLoggerFactory {
    override fun forClass(cls: KClass<*>): RARSLogger = NoopLogger
    override fun forName(name: String): RARSLogger = NoopLogger
    override fun forObject(obj: Any): RARSLogger = NoopLogger
}

@Suppress("UnusedReceiverParameter")
fun RARSLoggerFactory.noopLogger(): RARSLogger = NoopLogger
