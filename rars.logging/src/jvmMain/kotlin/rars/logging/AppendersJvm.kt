package rars.logging

import java.io.PrintStream

private class StreamAppender(
    private val stream: PrintStream,
    private val format: LoggingFormat
) : Appender {
    override fun append(
        context: LogContext
    ) = synchronized(stream) {
        val message = format.run { context.format() }
        stream.println(message)
    }
}

/**
 * Adds an [Appender] to the builder that will output the logging messages
 * to a given [PrintStream].
 */
fun LoggerFactoryBuilder.streamAppender(
    stream: PrintStream,
    format: LoggingFormat
): Unit = appender(StreamAppender(stream, format))

/**
 * Acts like [streamAppender], but for the [System.out] stream.
 */
fun LoggerFactoryBuilder.stdOutAppender(format: LoggingFormat) =
    streamAppender(System.out, format)

/**
 * Acts like [streamAppender], but for the [System.err] stream.
 */
fun LoggerFactoryBuilder.stdErrAppender(format: LoggingFormat) =
    streamAppender(System.err, format)
