package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.Lexer;
import rars.riscv.lang.lexing.RVLexer;
import rars.riscv.lang.lexing.RVTokenType;
import rars.riscv.lang.lexing.TokensProducer;

import javax.swing.text.Segment;

import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

@SuppressWarnings("unused")
public final class RSTATokensProducer extends TokenMakerBase implements TokensProducer<Token> {
    private static final String[] LINE_COMMENT_START_AND_END = {"#", null};
    private final Lexer<Token, RSTATokensProducer> lexer;

    public RSTATokensProducer(final Lexer<Token, RSTATokensProducer> lexer) {
        this.lexer = lexer;
    }

    public RSTATokensProducer() {
        this(new RVLexer<>());
    }

    @Override
    public void addToken(
        final char[] array, final int start, final int end, @NotNull final RVTokenType tokenType,
        final int startOffset
    ) {
        this.addToken(array, start, end, tokenValue(tokenType), startOffset);
    }

    @Override
    public Token getResult() {
        return firstToken;
    }

    @Override
    public Token getTokenList(final Segment text, final int initialTokenType, final int lineOffset, final int lineNum) {
        return getTokenList(text, initialTokenType, lineOffset);
    }

    @Override
    public Token getTokenList(final Segment segment, final int initialTokenType, final int initialOffset) {
        resetTokenList();
        return lexer.getTokensList(segment, initialTokenType, initialOffset, this);
    }

    @Override
    public @NotNull Token getEmptyResult() {
        return new TokenImpl();
    }

    @Override
    public String[] getLineCommentStartAndEnd(final int languageIndex) {
        return LINE_COMMENT_START_AND_END;
    }
}
