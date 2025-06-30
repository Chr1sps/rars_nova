package rars.logging

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val RarsLoggingFormat = LoggingFormat {
    val dateTime =
        timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = dateTime.date.toString()
    val time = dateTime.time.toString()
    val name = loggerName.coerceToSize(30)
    val method = source.methodName
    val lineNumber = source.lineNumber
    buildString {
        appendLine("[$date $time $name:$lineNumber] [$level] $message")
        exception?.let { appendLine(it.stackTraceToString()) }
    }
}

@JvmField
val RARSLogging = LoggerFactory.create {
    logLevel = LogLevel.INFO
    stdErrAppender(RarsLoggingFormat)
}

internal fun LoggerName.coerceToSize(charCount: Int): String = when (this) {
    is LoggerName.Class -> coerceToSize(charCount)
    is LoggerName.String -> name
}

private fun LoggerName.Class.coerceToSize(charCount: Int): String {
    val fqn = cls.name
    val initialLength = fqn.length
    if (initialLength <= charCount) return fqn
    val parts = fqn.split(".").toMutableList()
    val dotCount = parts.size - 1
    val targetSize = charCount - dotCount
    var currentSize = initialLength - dotCount
    parts.subList(
        0, parts.size - 1
    ).mutate { part ->
        val result =
            if (currentSize > targetSize) part.first().toString() else part
        currentSize = currentSize - (part.length - result.length)
        result
    }
    return parts.joinToString(".")
}

private fun <T> MutableList<T>.mutate(block: List<T>.(T) -> T): MutableList<T> =
    apply {
        for (i in indices) {
            this[i] = block(this[i])
        }
    }

fun Logger.requireOrError(condition: Boolean, message: () -> String) {
    if (!condition) {
        this.error(lazyMessage = message)
    }
}

fun Logger.requireOrWarn(condition: Boolean, message: () -> String) {
    if (!condition) {
        this.warning(lazyMessage = message)
    }
}
