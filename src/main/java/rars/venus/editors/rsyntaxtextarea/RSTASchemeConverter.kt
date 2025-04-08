package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.Style
import rars.riscv.lang.lexing.RVTokenType
import rars.util.applyStyle
import rars.venus.editors.TokenStyle
import rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue
import java.awt.Font

object RSTASchemeConverter {
    private fun convertStyle(style: TokenStyle, baseFont: Font): Style {
        return Style().apply {
            foreground = style.foreground
            background = style.background
            if (font == null) {
                font = baseFont
            }
            font = font.applyStyle(style)
            underline = style.isUnderline
        }
    }

    @JvmStatic
    fun convert(
        tokenStyles: Map<RVTokenType, TokenStyle>,
        baseFont: Font
    ): RVSyntaxScheme = RVSyntaxScheme().apply {
        tokenStyles.forEach { (key, value) ->
            val newKey = key.tokenValue
            val convertedStyle = convertStyle(value, baseFont)
            setStyle(newKey, convertedStyle)
        }
    }
}
