package rars.riscv.lang.lexing;

import javax.swing.text.Segment;

public interface TokensProducer<CollectionType> {
    void addToken(char[] array, int start, int end, RVTokenType tokenType,
                  int startOffset);

    default void addNullToken(char[] array, int segmentPos, int offset) {
        this.addToken(array, segmentPos, segmentPos, RVTokenType.NULL, offset);
    }

    default void addErrorToken(char[] array, int segmentPos, int offset, String notice) {
        this.addToken(array, segmentPos, segmentPos, RVTokenType.ERROR, offset);

    }

    CollectionType getResult();

    CollectionType getEmptyResult();

    CollectionType getTokenList(Segment text, int initialTokenType, int lineOffset, int lineNum);

}
