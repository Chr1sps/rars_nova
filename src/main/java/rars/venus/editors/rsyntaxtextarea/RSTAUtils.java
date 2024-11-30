package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class RSTAUtils {
    private static final Map<RVTokenType, Integer> tokenValueMap = ofEntries(
            entry(RVTokenType.NULL, Token.NULL),
            entry(RVTokenType.WHITESPACE, Token.WHITESPACE),
            entry(RVTokenType.COMMENT, Token.COMMENT_EOL),
            entry(RVTokenType.REGISTER_NAME, Token.VARIABLE),
            entry(RVTokenType.INTEGER, Token.LITERAL_NUMBER_DECIMAL_INT),
            entry(RVTokenType.FLOATING, Token.LITERAL_NUMBER_FLOAT),
            entry(RVTokenType.IDENTIFIER, Token.IDENTIFIER),
            entry(RVTokenType.STRING, Token.LITERAL_STRING_DOUBLE_QUOTE),
            entry(RVTokenType.CHAR, Token.LITERAL_CHAR),
            entry(RVTokenType.LABEL, Token.PREPROCESSOR),
            entry(RVTokenType.INSTRUCTION, Token.RESERVED_WORD),
            entry(RVTokenType.ERROR, Token.ERROR_IDENTIFIER),
            entry(RVTokenType.UNFINISHED_STRING, Token.ERROR_STRING_DOUBLE),
            entry(RVTokenType.UNFINISHED_CHAR, Token.ERROR_CHAR),
            entry(RVTokenType.OPERATOR, Token.OPERATOR)
    );

    private RSTAUtils() {
    }

    private static int extendedValue(final int value) {
        return value + Token.DEFAULT_NUM_TOKEN_TYPES;
    }

    public static int tokenValue(final @NotNull RVTokenType type) {
        if (tokenValueMap.containsKey(type)) {
            return tokenValueMap.get(type);
        }
        return extendedValue(type.ordinal());
    }

    public static int getSyntaxStyleTableSize() {
        return RVTokenType.values().length + Token.DEFAULT_NUM_TOKEN_TYPES;
    }
}
