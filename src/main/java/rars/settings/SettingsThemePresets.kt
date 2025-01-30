@file:Suppress("DuplicatedCode")

package rars.settings

import rars.venus.editors.TokenStyle
import java.awt.Color
import java.util.Map

object SettingsThemePresets {
    val LIGHT_THEME: SettingsTheme = buildTheme {
        foregroundColor = Color.BLACK
        backgroundColor = Color.WHITE
        lineHighlightColor = Color(242, 242, 242)
        selectionColor = Color(229, 235, 235)
        caretColor = Color.DARK_GRAY

        val errorColor = Color(255, 0, 0)
        val commentColor = Color(0, 128, 0)
        val directiveColor = Color(0, 134, 116)
        val registerColor = Color(169, 103, 0)
        val numberColor = Color(100, 0, 200)
        val stringColor = Color(175, 1, 121)
        val labelColor = Color(128, 128, 128)
        val instructionColor = Color(33, 33, 161)
        val macroParameterColor = Color(178, 49, 27)

        tokenStyles[TokenSettingKey.ERROR] = TokenStyle.underline(errorColor)
        tokenStyles[TokenSettingKey.COMMENT] = TokenStyle.italic(commentColor)
        tokenStyles[TokenSettingKey.DIRECTIVE] = TokenStyle.bold(directiveColor)

        tokenStyles[TokenSettingKey.REGISTER_NAME] = TokenStyle.bold(registerColor)

        tokenStyles[TokenSettingKey.NUMBER] = TokenStyle.bold(numberColor)
        tokenStyles[TokenSettingKey.STRING] = TokenStyle.bold(stringColor)

        tokenStyles[TokenSettingKey.LABEL] = TokenStyle.bold(labelColor)
        tokenStyles[TokenSettingKey.INSTRUCTION] = TokenStyle.bold(instructionColor)

        tokenStyles[TokenSettingKey.MACRO_PARAMETER] = TokenStyle.bold(macroParameterColor)
    }
    val DARK_THEME: SettingsTheme = buildTheme {
        foregroundColor = Color.WHITE
        backgroundColor = Color(31, 31, 31)
        lineHighlightColor = Color(47, 47, 47)
        selectionColor = Color(61, 61, 61)
        caretColor = Color.LIGHT_GRAY

        val errorColor = Color(255, 0, 0)
        val commentColor = Color(0, 159, 0)
        val directiveColor = Color(0, 183, 159)
        val registerColor = Color(222, 134, 0)
        val numberColor = Color(159, 57, 255)
        val stringColor = Color(255, 0, 176)
        val labelColor = Color(161, 161, 161)
        val instructionColor = Color(80, 80, 253)
        val macroParameterColor = Color(255, 70, 38)

        tokenStyles[TokenSettingKey.ERROR] = TokenStyle.underline(errorColor)
        tokenStyles[TokenSettingKey.COMMENT] = TokenStyle.italic(commentColor)
        tokenStyles[TokenSettingKey.DIRECTIVE] = TokenStyle.bold(directiveColor)

        tokenStyles[TokenSettingKey.REGISTER_NAME] = TokenStyle.bold(registerColor)

        tokenStyles[TokenSettingKey.NUMBER] = TokenStyle.bold(numberColor)
        tokenStyles[TokenSettingKey.STRING] = TokenStyle.bold(stringColor)

        tokenStyles[TokenSettingKey.LABEL] = TokenStyle.bold(labelColor)
        tokenStyles[TokenSettingKey.INSTRUCTION] = TokenStyle.bold(instructionColor)

        tokenStyles[TokenSettingKey.MACRO_PARAMETER] = TokenStyle.bold(macroParameterColor)
    }

    @JvmField
    val THEMES: List<ThemeEntry> = listOf(
        ThemeEntry("Default light", LIGHT_THEME),
        ThemeEntry("Default dark", DARK_THEME)
    )

    private fun buildTheme(builderFunction: SettingsTheme.() -> Unit): SettingsTheme = SettingsTheme(
        Color.WHITE,
        Color.WHITE,
        Color.WHITE,
        Color.WHITE,
        Color.WHITE,
        Map.of<TokenSettingKey, TokenStyle>()
    ).apply(builderFunction)

    @JvmRecord
    data class ThemeEntry(@JvmField val name: String, @JvmField val theme: SettingsTheme)
}
