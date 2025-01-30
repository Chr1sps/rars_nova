package rars.settings

import rars.riscv.lang.lexing.RVTokenType

/**
 * Sometimes it's nice to group a couple of different token types together in a
 * single color setting. This enum helps with that.
 */
enum class TokenSettingKey(@JvmField val description: String) {
    ERROR("Errors"),

    COMMENT("Comments"),
    DIRECTIVE("Directives"),

    REGISTER_NAME("Registers"),

    IDENTIFIER("Identifiers"),

    NUMBER("Numbers"),
    STRING("Strings"),

    LABEL("Labels"),
    INSTRUCTION("Instructions"),

    PUNCTUATION("Punctuation"),
    ROUNDING_MODE("Rounding modes"),
    MACRO_PARAMETER("Macro parameters"),
    HILO("%hi/%lo offsets");

    companion object {
        private val settingMappings = listOf(
            Pair(ERROR, RVTokenType.UNFINISHED_STRING),
            Pair(ERROR, RVTokenType.UNFINISHED_CHAR),
            Pair(ERROR, RVTokenType.ERROR),
            Pair(COMMENT, RVTokenType.COMMENT),
            Pair(DIRECTIVE, RVTokenType.DIRECTIVE),
            Pair(REGISTER_NAME, RVTokenType.REGISTER_NAME),
            Pair(IDENTIFIER, RVTokenType.IDENTIFIER),
            Pair(NUMBER, RVTokenType.INTEGER),
            Pair(NUMBER, RVTokenType.FLOATING),
            Pair(STRING, RVTokenType.STRING),
            Pair(STRING, RVTokenType.CHAR),
            Pair(LABEL, RVTokenType.LABEL),
            Pair(INSTRUCTION, RVTokenType.INSTRUCTION),
            Pair(PUNCTUATION, RVTokenType.PLUS),
            Pair(PUNCTUATION, RVTokenType.MINUS),
            Pair(PUNCTUATION, RVTokenType.COMMA),
            Pair(PUNCTUATION, RVTokenType.LEFT_PAREN),
            Pair(PUNCTUATION, RVTokenType.RIGHT_PAREN),
            Pair(PUNCTUATION, RVTokenType.OPERATOR),
            Pair(ROUNDING_MODE, RVTokenType.ROUNDING_MODE),
            Pair(MACRO_PARAMETER, RVTokenType.MACRO_PARAMETER),
            Pair(HILO, RVTokenType.HI),
            Pair(HILO, RVTokenType.LO)
        )

        @JvmStatic
        fun fromRVTokenType(type: RVTokenType): TokenSettingKey? = settingMappings.find { it.second == type }?.first

        @JvmStatic
        fun getTokenTypesForSetting(tokenSettingKey: TokenSettingKey): List<RVTokenType> =
            settingMappings.filter { it.first == tokenSettingKey }.map { it.second }.toList()
    }
}
