package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.Style
import org.fife.ui.rsyntaxtextarea.SyntaxScheme
import rars.riscv.lang.lexing.RVTokenType

class RVSyntaxScheme : SyntaxScheme(false) {
    init {
        val styles = arrayOfNulls<Style>(RVTokenType.entries.size + DEFAULT_NUM_TOKEN_TYPES)
        setStyles(styles)
        restoreDefaults(null)
    }
}
