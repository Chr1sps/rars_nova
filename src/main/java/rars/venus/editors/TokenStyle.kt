package rars.venus.editors

import java.awt.Color

@JvmRecord
data class TokenStyle(
    val foreground: Color?,
    val background: Color?,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isUnderline: Boolean
) : Cloneable {
    override fun clone(): TokenStyle = super.clone() as TokenStyle

    companion object {
        val DEFAULT_FOREGROUND: Color? = null
        val DEFAULT_BACKGROUND: Color? = null

        @JvmField
        val DEFAULT: TokenStyle = TokenStyle(
            DEFAULT_FOREGROUND, DEFAULT_BACKGROUND,
            isBold = false, isItalic = false, isUnderline = false
        )

        fun plain(foreground: Color): TokenStyle = TokenStyle(
            foreground, DEFAULT_BACKGROUND,
            isBold = false, isItalic = false, isUnderline = false
        )

        fun bold(foreground: Color): TokenStyle = TokenStyle(
            foreground, DEFAULT_BACKGROUND,
            isBold = true, isItalic = false, isUnderline = false
        )

        fun italic(foreground: Color): TokenStyle = TokenStyle(
            foreground, DEFAULT_BACKGROUND,
            isBold = false, isItalic = true, isUnderline = false
        )

        fun underline(foreground: Color): TokenStyle = TokenStyle(
            foreground, DEFAULT_BACKGROUND,
            isBold = false, isItalic = false, isUnderline = true
        )
    }
}
