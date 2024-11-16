package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import java.awt.*;

import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public class RVSyntaxScheme extends SyntaxScheme {
    private final Style @NotNull [] styles;

    public RVSyntaxScheme() {
        super(false);
        styles = new Style[RVTokenType.values().length + SyntaxScheme.DEFAULT_NUM_TOKEN_TYPES];
        setStyles(styles);
        restoreDefaults(null);
    }

    @Override
    public void restoreDefaults(final Font baseFont, final boolean fontStyles) {
        super.restoreDefaults(baseFont, fontStyles);
        final var error = new Color(255, 0, 0, 128);
        final var errorValue = tokenValue(RVTokenType.ERROR);
        styles[errorValue] = new Style(styles[errorValue].foreground, error);
    }
}
