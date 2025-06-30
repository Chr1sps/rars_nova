package rars.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NameTrimmingTests {
    @Test
    fun `no coercion needed`() =
        LoggerName.Class(javaClass)
            .assertCoercesTo(
                result = "rars.logging.NameTrimmingTests",
                size = 100
            )

    @Test
    fun `simple coercion`() =
        LoggerName.Class(javaClass)
            .assertCoercesTo(
                result = "r.l.NameTrimmingTests",
                size = 21,
            )

    @Test
    fun `doesnt coerce the class name itself`() =
        LoggerName.Class(javaClass)
            .assertCoercesTo(
                result = "r.l.NameTrimmingTests",
                size = 3,
            )

    @Test
    fun `doesnt coerce a custom name`() =
        LoggerName.String("CustomName").assertCoercesTo(
            result = "CustomName",
            size = 1
        )
}

private fun LoggerName.assertCoercesTo(result: String, size: Int) {
    val coerced = coerceToSize(size)
    assertEquals(result, coerced)
}
