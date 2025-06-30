package rars.riscv.lang.lexing

import rars.riscv.lang.Position
import kotlin.test.Test
import kotlin.test.assertContentEquals

class SomeLexingTest {
    @Test
    fun empty() = """
        
    """.tokenizesInto(

    )

    @Test
    fun comment() = """
        # comment
    """.tokenizesInto(
        RVToken(
            position = Position(line = 1, column = 0, offset = 0),
            type = RVTokenType.COMMENT,
            text = "# comment",
        )
    )
}

fun String.tokenizesInto(vararg expected: RVToken) {
    val lexer = createLexer(this.trimIndent())
    val tokens = lexer.intoSequence()
    val expectedTokens = expected.asSequence()
    assertContentEquals(expectedTokens, tokens, "Tokens are not equal.")
}

private fun createLexer(text: CharSequence): Lexer {
    TODO("Not yet implemented")
}

/**
 * Converts this lexer into a lazily evaluated sequence of tokens. Note that
 * this lexer is "moved" into this function and should no longer be used via
 * the original reference.
 */
private fun Lexer.intoSequence(): Sequence<RVToken> = sequence {
    while (currentToken != null) {
        yield(currentToken!!)
        advance()
    }
}