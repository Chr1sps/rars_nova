package rars.venus.settings.editor.views

import rars.settings.SettingsTheme
import rars.venus.util.BorderLayout
import rars.venus.util.CardLayout
import rars.venus.util.JPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.JPanel

class PickerCardView(
    initialTheme: SettingsTheme,
    initialCaretBlinkRate: Int,
    initialEditorTabSize: Int
) : JPanel() {

    val baseStyleView = BaseStyleView(initialTheme)
    val presetsView = PresetsView()
    val syntaxStyleView = SyntaxStyleView()
    val fontSettingsView = FontSettingsView()
    val otherSettingsView =
        OtherSettingsView(initialCaretBlinkRate, initialEditorTabSize)

    private val upperLayout: CardLayout
    private val upperPanel = JPanel {
        CardLayout {
            this[EMPTY] = JPanel()
            this[FONT] = fontSettingsView
            this[PRESETS] = presetsView
            this[BASE] = baseStyleView
            this[SYNTAX] = syntaxStyleView
            this[OTHER] = otherSettingsView
        }.also { upperLayout = it }
    }


    init {
        preferredSize = Dimension(450, 450)
        minimumSize = Dimension(50, 50)

        BorderLayout {
            this[BorderLayout.NORTH] = upperPanel
        }
    }

    fun showBaseStyleView() {
        upperLayout.show(upperPanel, BASE)
    }

    fun showFontView() {
        upperLayout.show(upperPanel, FONT)
    }

    fun showSyntaxStyleView() {
        upperLayout.show(upperPanel, SYNTAX)
    }

    fun showEmpty() {
        upperLayout.show(upperPanel, EMPTY)
    }

    fun showOtherSettings() {
        upperLayout.show(upperPanel, OTHER)
    }

    fun showPresets() {
        upperLayout.show(upperPanel, PRESETS)
    }

    companion object {
        private const val EMPTY = "empty"
        private const val FONT = "font"
        private const val PRESETS = "presets"
        private const val BASE = "base"
        private const val SYNTAX = "syntax"
        private const val OTHER = "other"
    }
}
