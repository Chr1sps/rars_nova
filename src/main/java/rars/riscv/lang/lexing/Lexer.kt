package rars.riscv.lang.lexing

import javax.swing.text.Segment

fun interface Lexer<T : Any?, P : TokensProducer<T>> {
    fun getTokensList(text: Segment, initialTokenType: Int, lineOffset: Int, producer: P): T
}
