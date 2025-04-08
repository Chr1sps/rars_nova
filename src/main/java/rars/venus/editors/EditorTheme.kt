package rars.venus.editors

import rars.riscv.lang.lexing.RVTokenType
import java.awt.Color

class EditorTheme(
    tokenStyles: Map<RVTokenType, TokenStyle>,
    @JvmField var backgroundColor: Color,
    @JvmField var foregroundColor: Color,
    @JvmField var lineHighlightColor: Color,
    @JvmField var caretColor: Color,
    @JvmField var selectionColor: Color
) {
    @JvmField
    var tokenStyles: MutableMap<RVTokenType, TokenStyle> = buildMap {
        RVTokenType.entries.forEach {
            put(it, tokenStyles[it] ?: TokenStyle.DEFAULT)
        }
    }.toMutableMap()
}
