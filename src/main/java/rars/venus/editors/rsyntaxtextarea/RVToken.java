package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RVToken(@NotNull Position position, @NotNull Type type, @NotNull String text) {

    public enum Type {
        NULL(Token.NULL),

        WHITESPACE(Token.WHITESPACE),
        COMMENT(Token.COMMENT_EOL),

        REGISTER_NAME,
        REGISTER_NUMBER,
        FLOATING_REGISTER_NAME,
        FLOATING_REGISTER_NUMBER,
        CSR_REGISTER_NAME,
        CSR_REGISTER_NUMBER,


        STRING(Token.LITERAL_STRING_DOUBLE_QUOTE),
        CHAR(Token.LITERAL_CHAR),
        LABEL(Token.PREPROCESSOR),
        INSTRUCTION(Token.RESERVED_WORD),
        DIRECTIVE,
        ERROR(Token.ERROR_IDENTIFIER),
        MACRO_ARG,
        UNFINISHED_STRING(Token.ERROR_STRING_DOUBLE),
        UNFINISHED_CHAR(Token.ERROR_CHAR),
        NUMBER(Token.LITERAL_NUMBER_DECIMAL_INT),
        OPERATOR(Token.OPERATOR),
        ROUNDING_MODE,
        HI,
        LO;

        public final int value;

        Type(final int value) {
            this.value = value;
        }

        Type() {
            this.value = extendedValue(this.ordinal());
        }

        public static int getSyntaxStyleTableSize() {
            return values().length + Token.DEFAULT_NUM_TOKEN_TYPES;
        }

        private static int extendedValue(final int value) {
            return value + Token.DEFAULT_NUM_TOKEN_TYPES;
        }

        private static @Nullable RVToken.Type fromInt(final int value) {
            final var enumVariants = RVToken.Type.class.getEnumConstants();
            for (final var variant : enumVariants) {
                if (variant.value == value) {
                    return variant;
                }
            }
            return null;
        }
    }

    public record Position(int offset, int line, int column) {
    }
}
