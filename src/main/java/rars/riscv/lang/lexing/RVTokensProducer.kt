package rars.riscv.lang.lexing

import rars.ErrorList
import rars.ErrorMessage
import rars.RISCVProgram
import rars.riscv.lang.Position
import javax.swing.text.Segment

class RVTokensProducer @JvmOverloads constructor(
    private val program: RISCVProgram,
    private val errorList: ErrorList,
    private val lexer: Lexer<TokenizedLine, RVTokensProducer> = RVLexer<TokenizedLine, RVTokensProducer>(),
) : TokensProducer<TokenizedLine> {
    private var lineNum = 0
    private var result = mutableListOf<RVToken>()

    override fun getEmptyResult(): TokenizedLine = TokenizedLine(emptyList(), lineNum)

    override fun getTokenList(
        text: Segment, initialTokenType: Int, lineOffset: Int,
        lineNum: Int
    ): TokenizedLine {
        this.result = ArrayList<RVToken>()
        this.lineNum = lineNum
        return lexer.getTokensList(text, initialTokenType, lineOffset, this)
    }

    override fun addToken(
        array: CharArray, start: Int, end: Int, tokenType: RVTokenType,
        startOffset: Int
    ) {
        val position = Position(lineNum, start, startOffset)
        val substring = String(array, start, end - start)
        val newToken = RVToken(position, tokenType, substring)
        result.add(newToken)
    }

    override fun addErrorToken(
        array: CharArray,
        segmentPos: Int,
        offset: Int,
        notice: String
    ) {
        this.errorList.add(ErrorMessage.error(program, lineNum, offset, notice))
        super.addErrorToken(array, segmentPos, offset, notice)
    }

    override fun getResult(): TokenizedLine = TokenizedLine(result, lineNum)
}
