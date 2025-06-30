package rars.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual interface LogContext {
    actual val timestamp: Instant
    actual val loggerName: LoggerName
    actual val message: Any?
    actual val exception: Throwable?
    actual val level: LogLevel
}

data class LogContextWasmJs(
    override val timestamp: Instant,
    override val loggerName: LoggerName,
    override val message: Any?,
    override val exception: Throwable?,
    override val level: LogLevel,
) : LogContext {
    companion object {
        fun create(
            name: LoggerName,
            message: Any?,
            exception: Throwable?,
            level: LogLevel,
        ): LogContextWasmJs = LogContextWasmJs(
            Clock.System.now(),
            name,
            message,
            exception,
            level,
        )
    }
}
