package rars.venus.editors

import rars.riscv.lang.lexing.RVTokenType
import java.awt.Color

class EditorTheme(
    tokenStyles: Map<RVTokenType, TokenStyle>,
    var backgroundColor: Color,
    var foregroundColor: Color,
    var lineHighlightColor: Color,
    var caretColor: Color,
    var selectionColor: Color
) {
    var tokenStyles: MutableMap<RVTokenType, TokenStyle> = buildMap {
        RVTokenType.entries.forEach {
            put(it, tokenStyles[it] ?: TokenStyle.DEFAULT)
        }
    }.toMutableMap()
}
