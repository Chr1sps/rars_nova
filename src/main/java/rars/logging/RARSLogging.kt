package rars.logging

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val RARSLoggerFactory = LoggerFactory.create {
    logLevel = LogLevel.WARNING
    stdErrAppender()
    logFormat {
        val dateTime =
            timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val date = dateTime.date.toString()
        val time = dateTime.time.toString()
        val name = loggerName.coerceToSize(20)
        buildString {
            appendLine("[$date $time $name] $level > $message")
            exception?.let { appendLine(it.stackTraceToString()) }
        }
    }
}

private fun LoggerName.coerceToSize(charCount: Int): String = when (this) {
    is LoggerName.ClassName -> coerceToSize(charCount)
    is LoggerName.StringName -> name
}

private fun LoggerName.ClassName.coerceToSize(charCount: Int): String {
    val fqn = cls.qualifiedName!!
    val initialLength = fqn.length
    if (initialLength <= charCount) return fqn
    val parts = fqn.split(".").toMutableList()
    val dotCount = parts.size - 1
    val targetSize = charCount - dotCount
    parts.subList(
        0, parts.size - 2
    ).mutate { part ->
        if (size > targetSize) {
            if (part.length == 1) {
                part
            } else {
                """${part.first()}${part.last()}"""
            }
        } else part
    }.mutate { part ->
        if (size > targetSize) part.first().toString() else part
    }
    return parts.joinToString(".")
}

private fun <T> MutableList<T>.mutate(block: List<T>.(T) -> T): MutableList<T> {
    for (i in indices) {
        this[i] = block(this[i])
    }
    return this
}

object RARSLogging : LoggerFactory by RARSLoggerFactory {
    @JvmStatic
    fun forJavaClass(cls: Class<*>) = RARSLoggerFactory.forJavaClass(cls)
}