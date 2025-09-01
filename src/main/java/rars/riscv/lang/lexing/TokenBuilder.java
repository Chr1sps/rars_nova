package rars.riscv.lang.lexing;

import javax.swing.text.Segment;

public interface TokenBuilder<CollectionType> {
    void addToken(
        char[] array, int start, int end, RVTokenType tokenType,
        int startOffset
    );

    default void addNullToken(final char[] array, final int segmentPos, final int offset) {
        this.addToken(array, segmentPos, segmentPos, RVTokenType.NULL, offset);
    }

    default void addErrorToken(final char[] array, final int segmentPos, final int offset, final String notice) {
        this.addToken(array, segmentPos, segmentPos, RVTokenType.ERROR, offset);
    }

    CollectionType getResult();

    CollectionType getEmptyResult();

    CollectionType getTokenList(Segment text, int initialTokenType, int lineOffset, int lineNum);
}
