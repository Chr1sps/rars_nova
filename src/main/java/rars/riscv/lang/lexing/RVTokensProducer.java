package rars.riscv.lang.lexing;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.Position;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.List;

public class RVTokensProducer implements TokensProducer<TokenizedLine> {
    private final Lexer<TokenizedLine, RVTokensProducer> lexer;
    private ArrayList<RVToken> result;
    private int lineNum;

    public RVTokensProducer(Lexer<TokenizedLine, RVTokensProducer> lexer) {
        this.lexer = lexer;
    }

    public RVTokensProducer() {
        this(new RVLexer<>());
    }

    @Override
    public @NotNull TokenizedLine getEmptyResult() {
        return new TokenizedLine(List.of(), lineNum);
    }

    @Override
    public TokenizedLine getTokenList(Segment text, int initialTokenType, int lineOffset, int lineNum) {
        this.result = new ArrayList<>();
        this.lineNum = lineNum;
        return lexer.getTokensList(text, initialTokenType, lineOffset, this);
    }

    @Override
    public void addToken(char @NotNull [] array, int start, int end, RVTokenType tokenType, int startOffset) {
        final var position = new Position(this.lineNum, start, startOffset);
        final var substring = new String(array, start, end - start);
        final var newToken = new RVToken(position, tokenType, substring);
        result.add(newToken);
    }

    @Override
    public @NotNull TokenizedLine getResult() {
        return new TokenizedLine(result, lineNum);
    }
}
