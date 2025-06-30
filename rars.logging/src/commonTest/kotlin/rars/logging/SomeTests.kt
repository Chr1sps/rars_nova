package rars.logging

import kotlin.test.*

class LoggerTests {

    @Test
    fun `no builder func body`() {
        assertFails {
            LoggerFactory.create {}
        }
    }

    @Test
    fun `only log level in builder func body`() {
        val factory = LoggerFactory.create {
            logLevel = LogLevel.WARNING
        }
        assertIs<NoopLoggerFactory>(factory)
        val logger = factory.forClass(LoggerTests::class)
        assertIs<NoopLogger>(logger)
    }

    @Test
    fun `only appender in builder func body`() {
        assertFails {
            LoggerFactory.create {
                appender { }
            }
        }
    }

    @Test
    fun `noop logger factory - log level is none`() {
        val stringBuilder = StringBuilder()
        val factory = LoggerFactory.create {
            logLevel = LogLevel.NONE
            stringBuilderAppender(stringBuilder, TestLogFormat)
        }
        assertIs<NoopLoggerFactory>(factory)
        val logger = factory.forClass(LoggerTests::class)
        assertIs<NoopLogger>(logger)
        logger.log(LogLevel.TRACE) { "message" }
        assertTrue { stringBuilder.isEmpty() }
    }

    @Test
    fun `simple logger test`() {
        val builder = StringBuilder()
        val factory = LoggerFactory.create {
            logLevel = LogLevel.INFO
            stringBuilderAppender(builder, TestLogFormat)
        }
        val logger = factory.forName("test")
        assertIsNot<NoopLogger>(logger)
        logger.log(LogLevel.INFO) { "message" } // should be logged
        logger.log(LogLevel.DEBUG) { "message" } // should not be logged
        assertEquals("[INFO] message\n", builder.toString())
    }
}

private val TestLogFormat = LoggingFormat { "[$level] $message\n" }

private fun LoggerFactoryBuilder.stringBuilderAppender(
    builder: StringBuilder,
    format: LoggingFormat
) = formattedAppender(format) { builder.append(it) }