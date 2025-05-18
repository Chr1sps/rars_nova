package rars.riscv.lang.lexing

import rars.riscv.lang.Position

interface Lexer {
    /** The current token that the lexer points to, or `null` if EOF. */
    val currentToken: RVToken?

    /** The full text whose subsequence is being lexed. */
    val originalText: CharSequence

    /** Advances the lexer to the next token. */
    fun advance()

    /**
     * Rolls back the lexer to a previous position.
     * @param previousPosition the position to roll back to.
     */
    fun restore(previousPosition: LexerPosition)
}

fun interface LexerFactory {
    fun create(
        text: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int
    ): Lexer
}

interface LexerPosition {
    val position: Position
    val state: Int
}

interface TokenList {
    val tokenCount: Int
    val tokenizedText: CharSequence
    operator fun get(index: Int): RVToken?
}

private class TokenListImpl internal constructor(
    private val lexStarts: Array<Position>,
    private val lexTypes: Array<RVTokenType>,
    override val tokenCount: Int,
    override val tokenizedText: CharSequence,
) : TokenList {
    override fun get(index: Int): RVToken? =
        if (index < 0 || index >= tokenCount) {
            null
        } else RVToken(
            lexStarts[index],
            lexTypes[index],
            tokenizedText.subSequence(
                lexStarts[index].offset,
                lexStarts[index + 1].offset
            )
        )
}
//fun LexerFactory.lexLine(
//    line: CharSequence,
//    startOffset: Int,
//    lineNumber: Int,
//): Sequence<RVToken> {
//    val lexer = create(line, 0, line.length, 0)
//    lexer.advance()
//    return generateSequence {
//        val nextToken = lexer.currentToken
//        lexer.advance()
//        nextToken
//    }
//}