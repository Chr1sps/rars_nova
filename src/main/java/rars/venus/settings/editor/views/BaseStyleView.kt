package rars.venus.settings.editor.views

import rars.settings.SettingsTheme
import rars.venus.settings.editor.ColorPickerButton
import rars.venus.settings.editor.OptionSection
import rars.venus.util.BoxLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class BaseStyleView(initialTheme: SettingsTheme) : JPanel() {
    val foregroundColorButton: ColorPickerButton
    val backgroundColorButton: ColorPickerButton
    val lineHighlightColorButton: ColorPickerButton
    val textSelectionColorButton: ColorPickerButton
    val caretColorButton: ColorPickerButton

    init {

        // foreground
        val foregroundSection =
            OptionSection(FOREGROUND, null, initialTheme.foregroundColor)
        foregroundColorButton = foregroundSection.colorPickerButton!!

        // background
        val backgroundSection =
            OptionSection(BACKGROUND, null, initialTheme.backgroundColor)
        backgroundColorButton = backgroundSection.colorPickerButton!!

        // line highlight
        val lineHighlightSection =
            OptionSection(LINE_HIGHLIGHT, null, initialTheme.lineHighlightColor)
        lineHighlightColorButton = lineHighlightSection.colorPickerButton!!

        // text selection
        val textSelectionSection =
            OptionSection(TEXT_SELECTION, null, initialTheme.selectionColor)
        textSelectionColorButton = textSelectionSection.colorPickerButton!!

        // caret
        val caretSection = OptionSection(CARET, null, initialTheme.caretColor)
        caretColorButton = caretSection.colorPickerButton!!

        val topRow = buildRow(
            addMargins = false,
            foregroundSection,
            backgroundSection,
            lineHighlightSection
        )
        val bottomRow =
            buildRow(addMargins = true, textSelectionSection, caretSection)

        BoxLayout(BoxLayout.Y_AXIS) {
            +topRow
            verticalStrut(5)
            +bottomRow
            verticalGlue()
        }
    }
}

private const val FOREGROUND = "Foreground"
private const val BACKGROUND = "Background"
private const val LINE_HIGHLIGHT = "Line highlight"
private const val CARET = "Caret"
private const val TEXT_SELECTION = "Text selection"
