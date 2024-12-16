package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.riscv.lang.lexing.RVTokenType;

public enum TokenSettingKey {
    ERROR,

    COMMENT,
    DIRECTIVE,

    REGISTER_NAME,

    IDENTIFIER,

    INTEGER,
    FLOATING,
    STRING,
    CHAR,

    LABEL,
    INSTRUCTION,

    PUNCTUATION,
    ROUNDING_MODE,
    MACRO_PARAMETER,
    HILO;

    public static @Nullable TokenSettingKey fromRVTokenType(final @NotNull RVTokenType type) {
        return switch (type) {
            case UNFINISHED_STRING, UNFINISHED_CHAR, ERROR -> ERROR;
            case COMMENT -> COMMENT;
            case DIRECTIVE -> DIRECTIVE;
            case REGISTER_NAME -> REGISTER_NAME;
            case IDENTIFIER -> IDENTIFIER;
            case INTEGER -> INTEGER;
            case FLOATING -> FLOATING;
            case STRING -> STRING;
            case CHAR -> CHAR;
            case LABEL -> LABEL;
            case INSTRUCTION -> INSTRUCTION;
            case PLUS, MINUS, COMMA, LEFT_PAREN, RIGHT_PAREN, OPERATOR -> PUNCTUATION;
            case ROUNDING_MODE -> ROUNDING_MODE;
            case MACRO_PARAMETER -> MACRO_PARAMETER;
            case HI, LO -> HILO;
            default -> null;
        };
    }
}
