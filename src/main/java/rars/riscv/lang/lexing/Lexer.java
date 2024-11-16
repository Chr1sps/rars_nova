package rars.riscv.lang.lexing;

import javax.swing.text.Segment;

public interface Lexer<T, P extends TokensProducer<T>> {
    T getTokensList(Segment text, int initialTokenType, int lineOffset, P producer);
}
