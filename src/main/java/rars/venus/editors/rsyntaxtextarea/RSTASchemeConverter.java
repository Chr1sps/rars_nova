package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Style;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.venus.editors.TokenStyle;

import java.awt.*;
import java.util.Map;

import static rars.util.Utils.deriveFontFromStyle;
import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public final class RSTASchemeConverter {
    private RSTASchemeConverter() {
    }

    private static @NotNull Style convertStyle(final @NotNull TokenStyle style, final @NotNull Font baseFont) {
        final var result = new Style();
        result.foreground = style.foreground();
        result.background = style.background();
        if (result.font == null)
            result.font = baseFont;
        result.font = deriveFontFromStyle(result.font, style);
        result.underline = style.isUnderline();
        return result;
    }

    public static @NotNull RVSyntaxScheme convert(final @NotNull Map<@NotNull RVTokenType, @NotNull TokenStyle> tokenStyles,
                                                  final @NotNull Font baseFont) {
        final var result = new RVSyntaxScheme();
        tokenStyles.forEach((key, value) -> {
            final var newKey = tokenValue(key);
            final var convertedStyle = convertStyle(value, baseFont);
            result.setStyle(newKey, convertedStyle);
        });
        return result;
    }
}
