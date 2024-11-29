package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import java.awt.*;

import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public class RVSyntaxScheme extends SyntaxScheme {
    public RVSyntaxScheme() {
        super(false);
        final var styles = new Style[RVTokenType.values().length + SyntaxScheme.DEFAULT_NUM_TOKEN_TYPES];
        setStyles(styles);
        restoreDefaults(null);
    }
}
