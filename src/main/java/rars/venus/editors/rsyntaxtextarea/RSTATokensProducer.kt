package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenImpl
import org.fife.ui.rsyntaxtextarea.TokenMakerBase
import rars.riscv.lang.lexing.Lexer
import rars.riscv.lang.lexing.RVLexer
import rars.riscv.lang.lexing.RVTokenType
import rars.riscv.lang.lexing.TokensProducer
import rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue
import javax.swing.text.Segment

@Suppress("unused")
class RSTATokensProducer @JvmOverloads constructor(
    private val lexer: Lexer<Token, RSTATokensProducer> = RVLexer<Token, RSTATokensProducer>()
) : TokenMakerBase(), TokensProducer<Token> {
    override fun addToken(
        array: CharArray, start: Int, end: Int, tokenType: RVTokenType,
        startOffset: Int
    ) {
        addToken(array, start, end, tokenType.tokenValue, startOffset)
    }

    override fun getResult(): Token = firstToken

    override fun getTokenList(
        text: Segment,
        initialTokenType: Int,
        lineOffset: Int,
        lineNum: Int
    ): Token = getTokenList(text, initialTokenType, lineOffset)

    override fun getTokenList(segment: Segment, initialTokenType: Int, initialOffset: Int): Token {
        resetTokenList()
        return lexer.getTokensList(segment, initialTokenType, initialOffset, this)
    }

    override fun getEmptyResult(): Token = TokenImpl()

    override fun getLineCommentStartAndEnd(languageIndex: Int): Array<String?> = LINE_COMMENT_START_AND_END

    companion object {
        private val LINE_COMMENT_START_AND_END: Array<String?> = arrayOf("#", null)
    }
}
