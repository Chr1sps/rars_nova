package rars.riscv.lang.lexing

class RVLexer private constructor(
    override val originalText: CharSequence,
    val maxIdentifierLength: Int,
) : Lexer {
    private var state: Int = 0
    private var offset: Int = 0

    init {

    }

    override val currentToken: RVToken?
        get() = TODO("Not yet implemented")

    override fun advance() {
        TODO("Not yet implemented")
    }

    override fun restore(previousPosition: LexerPosition) {
        TODO("Not yet implemented")
    }

    override val lexerPosition: LexerPosition
        get() = LexerPositionImpl(offset, state)

    companion object {
        fun fromCharSequence(
            text: CharSequence,
            maxIdentifierLength: Int = 64
        ): RVLexer = RVLexer(text, maxIdentifierLength)
    }
}

private data class LexerPositionImpl(
    override val offset: Int,
    override val state: Int
) : LexerPosition