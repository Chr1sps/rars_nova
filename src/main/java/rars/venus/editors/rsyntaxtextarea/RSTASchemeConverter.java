package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Style;
import org.jetbrains.annotations.NotNull;
import rars.venus.editors.ColorScheme;
import rars.venus.editors.ColorSchemeConverter;
import rars.venus.editors.TokenStyle;

import java.awt.*;

import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public final class RSTASchemeConverter implements ColorSchemeConverter<RVSyntaxScheme> {
    public static final RSTASchemeConverter INSTANCE = new RSTASchemeConverter();

    private RSTASchemeConverter() {
    }

    private static @NotNull Style convertStyle(final @NotNull TokenStyle style, final @NotNull Font baseFont) {
        final var result = new Style();
        result.foreground = style.foreground();
        result.background = style.background();
        var fontStyle = 0;
        if (style.isBold()) fontStyle |= Font.BOLD;
        if (style.isItalic()) fontStyle |= Font.ITALIC;
        //noinspection MagicConstant
        if (result.font == null)
            result.font = baseFont;
        result.font = result.font.deriveFont(fontStyle);
        result.underline = style.isUnderline();
        return result;
    }

    @Override
    public @NotNull RVSyntaxScheme convert(final @NotNull ColorScheme colorScheme, final @NotNull Font baseFont) {
        final var result = new RVSyntaxScheme();
        for (final var entry : colorScheme.getStyleMap().entrySet()) {
            final var newKey = tokenValue(entry.getKey());
            final var convertedStyle = convertStyle(entry.getValue(), baseFont);
            result.setStyle(newKey, convertedStyle);
        }
        return result;
    }
}
