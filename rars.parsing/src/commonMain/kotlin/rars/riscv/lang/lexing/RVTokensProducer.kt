package rars.riscv.lang.lexing

//class RVTokensProducer(
//    private val lexer: Lexer<TokenizedLine, RVTokensProducer>, /*= RVLexer<TokenizedLine, RVTokensProducer>()*/
//    private val logger: Logger,
//) : TokensProducer<TokenizedLine> {
//    private var lineNum = 0
//    private var result = mutableListOf<RVToken>()
//
//    override fun getEmptyResult(): TokenizedLine =
//        TokenizedLine(emptyList(), lineNum)
//
//    override fun getTokenList(
//        text: CharSequence, initialTokenType: Int, lineOffset: Int,
//        lineNum: Int
//    ): TokenizedLine {
//        result = mutableListOf()
//        this.lineNum = lineNum
//        return lexer.getTokensList(text, initialTokenType, lineOffset, this)
//    }
//
//    override fun addToken(
//        array: CharArray, start: Int, end: Int, tokenType: RVTokenType,
//        startOffset: Int
//    ) {
//        val position = Position(lineNum, start, startOffset)
//        val substring = array.concatToString(start, start + (end - start))
//        val newToken = RVToken(position, tokenType, substring)
//        result.add(newToken)
//    }
//
//    override fun addErrorToken(
//        array: CharArray,
//        segmentPos: Int,
//        offset: Int,
//        notice: String
//    ) {
//        logger.error { notice }
////        this.errorList.add(ErrorMessage.error(program, lineNum, offset, notice))
//        super.addErrorToken(array, segmentPos, offset, notice)
//    }
//
//    override fun getResult(): TokenizedLine = TokenizedLine(result, lineNum)
//}
