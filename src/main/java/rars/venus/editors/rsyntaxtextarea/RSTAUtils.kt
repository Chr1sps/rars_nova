package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.Token
import rars.riscv.lang.lexing.RVTokenType

object RSTAUtils {
    private val TOKEN_VALUE_MAP = mapOf(
        RVTokenType.NULL to Token.NULL,
        RVTokenType.WHITESPACE to Token.WHITESPACE,
        RVTokenType.COMMENT to Token.COMMENT_EOL,
        RVTokenType.REGISTER_NAME to Token.VARIABLE,
        RVTokenType.INTEGER to Token.LITERAL_NUMBER_DECIMAL_INT,
        RVTokenType.FLOATING to Token.LITERAL_NUMBER_FLOAT,
        RVTokenType.IDENTIFIER to Token.IDENTIFIER,
        RVTokenType.STRING to Token.LITERAL_STRING_DOUBLE_QUOTE,
        RVTokenType.CHAR to Token.LITERAL_CHAR,
        RVTokenType.LABEL to Token.PREPROCESSOR,
        RVTokenType.INSTRUCTION to Token.RESERVED_WORD,
        RVTokenType.ERROR to Token.ERROR_IDENTIFIER,
        RVTokenType.UNFINISHED_STRING to Token.ERROR_STRING_DOUBLE,
        RVTokenType.UNFINISHED_CHAR to Token.ERROR_CHAR,
        RVTokenType.OPERATOR to Token.OPERATOR
    )

    private fun Int.extendedValue(): Int = this + Token.DEFAULT_NUM_TOKEN_TYPES

    @JvmStatic
    val RVTokenType.tokenValue
        get() = this@RSTAUtils.TOKEN_VALUE_MAP[this]
            ?: ordinal.extendedValue()
}
