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
        assertFails {
            LoggerFactory.create {
                logLevel = LogLevel.WARNING
            }
        }
    }

    @Test
    fun `only appender in builder func body`() {
        assertFails {
            LoggerFactory.create {
                appender { _, _ -> }
            }
        }
    }

    @Test
    fun `noop logger factory - no appenders`() {
        val factory = LoggerFactory.create {
            logLevel = LogLevel.WARNING
            logFormat {
                "message"
            }
        }
        assertIs<NoopLoggerFactory>(factory)
        val logger = factory.forClass(LoggerTests::class)
        assertIs<NoopLogger>(logger)
    }

    @Test
    fun `noop logger factory - log level is none`() {
        val stringBuilder = StringBuilder()
        val factory = LoggerFactory.create {
            logLevel = LogLevel.NONE
            appender { message, _ ->
                stringBuilder.append(message)
            }
            logFormat { message.toString() }
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
            appender { message, _ ->
                builder.appendLine(message)
            }
            logFormat {
                """[$level] $message"""
            }
        }
        val logger = factory.forName("test")
        assertIsNot<NoopLogger>(logger)
        logger.log(LogLevel.INFO) { "message" } // should be logged
        logger.log(LogLevel.DEBUG) { "message" } // should not be logged
        assertEquals("[INFO] message\n", builder.toString())
    }
}