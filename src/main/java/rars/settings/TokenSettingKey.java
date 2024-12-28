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
    
    public final @NotNull String description;

    private static final @NotNull List<@NotNull Pair<@NotNull TokenSettingKey, @NotNull RVTokenType>> settingMappings = List.of(
        new Pair<>(ERROR, RVTokenType.UNFINISHED_STRING),
        new Pair<>(ERROR, RVTokenType.UNFINISHED_CHAR),
        new Pair<>(ERROR, RVTokenType.ERROR),
        new Pair<>(COMMENT, RVTokenType.COMMENT),
        new Pair<>(DIRECTIVE, RVTokenType.DIRECTIVE),
        new Pair<>(REGISTER_NAME, RVTokenType.REGISTER_NAME),
        new Pair<>(IDENTIFIER, RVTokenType.IDENTIFIER),
        new Pair<>(NUMBER, RVTokenType.INTEGER),
        new Pair<>(NUMBER, RVTokenType.FLOATING),
        new Pair<>(STRING, RVTokenType.STRING),
        new Pair<>(STRING, RVTokenType.CHAR),
        new Pair<>(LABEL, RVTokenType.LABEL),
        new Pair<>(INSTRUCTION, RVTokenType.INSTRUCTION),
        new Pair<>(PUNCTUATION, RVTokenType.PLUS),
        new Pair<>(PUNCTUATION, RVTokenType.MINUS),
        new Pair<>(PUNCTUATION, RVTokenType.COMMA),
        new Pair<>(PUNCTUATION, RVTokenType.LEFT_PAREN),
        new Pair<>(PUNCTUATION, RVTokenType.RIGHT_PAREN),
        new Pair<>(PUNCTUATION, RVTokenType.OPERATOR),
        new Pair<>(ROUNDING_MODE, RVTokenType.ROUNDING_MODE),
        new Pair<>(MACRO_PARAMETER, RVTokenType.MACRO_PARAMETER),
        new Pair<>(HILO, RVTokenType.HI),
        new Pair<>(HILO, RVTokenType.LO)
    );

    TokenSettingKey(@NotNull String description) {
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
