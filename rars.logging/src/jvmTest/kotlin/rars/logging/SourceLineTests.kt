package rars.logging

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLineTests {
    @Test
    fun `test source line`() {
        val stringBuilder = StringBuilder()
        val factory = LoggerFactory.create {
            logLevel = LogLevel.INFO
            stringBuilderAppender(stringBuilder, LineNumberFormat)
        }
        val logger = factory.forName("test")
        logger.info {}
        logger.info {}

        logger.info {}
        assertEquals("15\n16\n18\n", stringBuilder.toString())
    }
}

private fun LoggerFactoryBuilder.stringBuilderAppender(
    builder: StringBuilder,
    format: LoggingFormat
) = formattedAppender(format) { builder.append(it) }

private val LineNumberFormat = LoggingFormat { "${source.lineNumber}\n" }