package rars.riscv.lang.lexing;

import javax.swing.text.Segment;

public interface Lexer<T, P extends TokenBuilder<T>> {
    T getTokensList(Segment text, int initialTokenType, int lineOffset, P producer);
}
