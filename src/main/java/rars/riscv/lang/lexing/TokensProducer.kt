package rars.riscv.lang.lexing

import javax.swing.text.Segment

interface TokensProducer<CollectionType : Any?> {
    fun addToken(
        array: CharArray, start: Int, end: Int, tokenType: RVTokenType,
        startOffset: Int
    )

    fun addNullToken(array: CharArray, segmentPos: Int, offset: Int) {
        addToken(array, segmentPos, segmentPos, RVTokenType.NULL, offset)
    }

    fun addErrorToken(array: CharArray, segmentPos: Int, offset: Int, notice: String) {
        addToken(array, segmentPos, segmentPos, RVTokenType.ERROR, offset)
    }

    fun getResult(): CollectionType

    fun getEmptyResult(): CollectionType

    fun getTokenList(text: Segment, initialTokenType: Int, lineOffset: Int, lineNum: Int): CollectionType
}
