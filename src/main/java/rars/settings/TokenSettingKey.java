package rars.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import rars.riscv.lang.lexing.RVTokenType;
import rars.util.Pair;

import java.util.List;

/**
 * Sometimes it's nice to group a couple of different token types together in a
 * single color setting. This enum helps with that.
 */
public enum TokenSettingKey {
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

    private static final @NotNull List<@NotNull Pair<@NotNull TokenSettingKey, @NotNull RVTokenType>> settingMappings = List.of(
        Pair.of(ERROR, RVTokenType.UNFINISHED_STRING),
        Pair.of(ERROR, RVTokenType.UNFINISHED_CHAR),
        Pair.of(ERROR, RVTokenType.ERROR),
        Pair.of(COMMENT, RVTokenType.COMMENT),
        Pair.of(DIRECTIVE, RVTokenType.DIRECTIVE),
        Pair.of(REGISTER_NAME, RVTokenType.REGISTER_NAME),
        Pair.of(IDENTIFIER, RVTokenType.IDENTIFIER),
        Pair.of(NUMBER, RVTokenType.INTEGER),
        Pair.of(NUMBER, RVTokenType.FLOATING),
        Pair.of(STRING, RVTokenType.STRING),
        Pair.of(STRING, RVTokenType.CHAR),
        Pair.of(LABEL, RVTokenType.LABEL),
        Pair.of(INSTRUCTION, RVTokenType.INSTRUCTION),
        Pair.of(PUNCTUATION, RVTokenType.PLUS),
        Pair.of(PUNCTUATION, RVTokenType.MINUS),
        Pair.of(PUNCTUATION, RVTokenType.COMMA),
        Pair.of(PUNCTUATION, RVTokenType.LEFT_PAREN),
        Pair.of(PUNCTUATION, RVTokenType.RIGHT_PAREN),
        Pair.of(PUNCTUATION, RVTokenType.OPERATOR),
        Pair.of(ROUNDING_MODE, RVTokenType.ROUNDING_MODE),
        Pair.of(MACRO_PARAMETER, RVTokenType.MACRO_PARAMETER),
        Pair.of(HILO, RVTokenType.HI),
        Pair.of(HILO, RVTokenType.LO)
    );
    public final @NotNull String description;

    TokenSettingKey(@NotNull final String description) {
        this.description = description;
    }

    public static @Nullable TokenSettingKey fromRVTokenType(final @NotNull RVTokenType type) {
        return settingMappings.stream()
            .filter(pair -> pair.second() == type)
            .map(Pair::first)
            .findAny()
            .orElse(null);
    }

    public static @NotNull @Unmodifiable List<@NotNull RVTokenType> getTokenTypesForSetting(final @NotNull TokenSettingKey tokenSettingKey) {
        return settingMappings.stream()
            .filter(pair -> pair.first() == tokenSettingKey)
            .map(Pair::second)
            .toList();
    }
}
