package rars.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.reflect.jvm.javaMethod

private val loggerClassNames = arrayOf<String>(
    AbstractLogger::class.java.name,
    Logger::class.java.name,
    Logger::info.javaMethod!!.declaringClass.name,
)

private fun Array<StackTraceElement>.findLoggerCallFrame(): StackTraceElement? =
    indexOfLast { it.className in loggerClassNames }
        .takeIf { it in 0..<(size - 1) }
        ?.let { get(it + 1) }

actual interface LogContext {
    actual val timestamp: Instant
    actual val loggerName: LoggerName
    actual val message: Any?
    actual val exception: Throwable?
    actual val level: LogLevel
    val threadInfo: ThreadInfo

    /** The stack frame of the logger call that occurred. */
    val source: StackTraceElement
}

class LogContextJvm(
    override val timestamp: Instant,
    override val loggerName: LoggerName,
    override val message: Any?,
    override val exception: Throwable?,
    override val level: LogLevel,
    private val thread: Thread,
) : LogContext {
    override val threadInfo: ThreadInfo = thread.threadInfo
    override val source: StackTraceElement by lazy {
        thread.stackTrace.findLoggerCallFrame() ?: error {
            "Failed to find source for logger context."
        }
    }

    companion object {
        fun create(
            name: LoggerName,
            message: Any?,
            exception: Throwable?,
            level: LogLevel,
        ): LogContextJvm = LogContextJvm(
            Clock.System.now(),
            name,
            message,
            exception,
            level,
            Thread.currentThread(),
        )
    }
}

/**
 * Contains information about a given thread at the moment of a logging message
 * occuring.
 */
interface ThreadInfo {
    val name: String
    val id: Long
    val state: Thread.State
    val stackTrace: Array<StackTraceElement>
    val isDaemon: Boolean
    val priority: Int
    val isVirtual: Boolean
    val isInterrupted: Boolean
}

val Thread.threadInfo
    get() = let { thread ->
        object : ThreadInfo {
            override val name: String = thread.name
            override val id: Long = thread.threadId()
            override val state: Thread.State = thread.state
            override val stackTrace: Array<StackTraceElement> by lazy { thread.stackTrace.copyOf() }
            override val isDaemon: Boolean = thread.isDaemon
            override val priority: Int = thread.priority
            override val isVirtual: Boolean = thread.isVirtual
            override val isInterrupted: Boolean = thread.isInterrupted
        }
    }
