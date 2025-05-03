package rars.logging

import java.io.PrintStream

class StreamAppender(private val stream: PrintStream) : Appender {
    override fun append(
        message: String,
        logLevel: LogLevel
    ) = synchronized(stream) {
        stream.println(message)
    }
}

object ConsoleAppenders {
    val STDOUT = StreamAppender(System.out)
    val STDERR = StreamAppender(System.err)
}

fun LoggerFactoryBuilder.stdOutAppender() {
    appender(ConsoleAppenders.STDOUT)
}

fun LoggerFactoryBuilder.stdErrAppender() {
    appender(ConsoleAppenders.STDERR)
}

fun LoggerFactory.forJavaClass(cls: Class<*>): Logger = forClass(cls.kotlin)