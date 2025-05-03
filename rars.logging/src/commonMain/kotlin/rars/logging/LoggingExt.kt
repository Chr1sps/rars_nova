package rars.logging

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

@JvmOverloads
@JvmName("logFatal")
fun Logger.fatal(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.FATAL, exception, lazyMessage)

@JvmOverloads
@JvmName("logError")
fun Logger.error(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.ERROR, exception, lazyMessage)

@JvmOverloads
@JvmName("logWarning")
fun Logger.warning(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.WARNING, exception, lazyMessage)

@JvmOverloads
@JvmName("logInfo")
fun Logger.info(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.INFO, exception, lazyMessage)

@JvmOverloads
@JvmName("logDebug")
fun Logger.debug(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.DEBUG, exception, lazyMessage)

@JvmOverloads
@JvmName("logTrace")
fun Logger.trace(exception: Throwable? = null, lazyMessage: () -> Any?) =
    log(LogLevel.TRACE, exception, lazyMessage)
