package rars.settings

import rars.riscv.lang.lexing.RVTokenType
import rars.venus.editors.EditorTheme
import rars.venus.editors.TokenStyle
import java.awt.Color

class SettingsTheme(
    @JvmField var backgroundColor: Color,
    @JvmField var foregroundColor: Color,
    @JvmField var lineHighlightColor: Color,
    @JvmField var caretColor: Color,
    @JvmField var selectionColor: Color,
    tokenStyles: Map<TokenSettingKey, TokenStyle>
) : Cloneable {
    @JvmField
    var tokenStyles: MutableMap<TokenSettingKey, TokenStyle>

    init {
        this.tokenStyles = getFilledMap(tokenStyles)
    }

    fun toEditorTheme(): EditorTheme {
        return EditorTheme(
            convertSettingsToThemeTokenStyles(this.tokenStyles),
            this.backgroundColor,
            this.foregroundColor,
            this.lineHighlightColor,
            this.caretColor,
            this.selectionColor
        )
    }

    public override fun clone(): SettingsTheme {
        val clone: SettingsTheme
        try {
            clone = super.clone() as SettingsTheme
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }

        clone.backgroundColor = Color(this.backgroundColor.rgb, false)
        clone.foregroundColor = Color(this.foregroundColor.rgb, false)
        clone.lineHighlightColor = Color(this.lineHighlightColor.rgb, false)
        clone.caretColor = Color(this.caretColor.rgb, false)
        clone.selectionColor = Color(this.selectionColor.rgb, false)

        clone.tokenStyles = HashMap<TokenSettingKey, TokenStyle>(this.tokenStyles)

        return clone
    }

    companion object {
        @JvmField
        var DEFAULT_THEME: SettingsTheme = SettingsThemePresets.LIGHT_THEME

        private fun convertSettingsToThemeTokenStyles(
            tokenStyles: MutableMap<TokenSettingKey, TokenStyle>
        ) = mutableMapOf<RVTokenType, TokenStyle>().apply {
            tokenStyles.forEach { (key, style) ->
                TokenSettingKey
                    .getTokenTypesForSetting(key)
                    .forEach { tokenType -> this[tokenType] = style }
            }
        }

        private fun getFilledMap(baseMap: Map<TokenSettingKey, TokenStyle>): MutableMap<TokenSettingKey, TokenStyle> {
            val result = baseMap.toMutableMap()
            TokenSettingKey.entries.forEach { key ->
                result.putIfAbsent(key, TokenStyle.DEFAULT)
            }
            return result
        }
    }
}
