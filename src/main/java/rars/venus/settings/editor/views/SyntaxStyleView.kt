package rars.venus.settings.editor.views

import rars.util.intersperseWith
import rars.venus.settings.editor.ColorPickerButton
import rars.venus.settings.editor.OptionSection
import rars.venus.util.BoxLayout
import rars.venus.util.JPanel
import java.awt.Color
import javax.swing.*

class SyntaxStyleView : JPanel() {
    val isBold: JCheckBox
    val isItalic: JCheckBox
    val isUnderline: JCheckBox
    val useForeground: JCheckBox
    val useBackground: JCheckBox
    val foregroundColorButton: ColorPickerButton
    val backgroundColorButton: ColorPickerButton

    init {

        // foreground
        val foregroundSection = OptionSection("Foreground", false, Color.BLACK)
        useForeground = foregroundSection.checkBox!!
        foregroundColorButton = foregroundSection.colorPickerButton!!

        // background
        val backgroundSection = OptionSection("Background", false, Color.WHITE)
        useBackground = backgroundSection.checkBox!!
        backgroundColorButton = backgroundSection.colorPickerButton!!

        // bold
        val boldSection = OptionSection("Bold", false, null)
        isBold = boldSection.checkBox!!

        // italic
        val italicSection = OptionSection("Italic", false, null)
        isItalic = italicSection.checkBox!!

        // underline
        val underlineSection = OptionSection("Underline", false, null)
        isUnderline = underlineSection.checkBox!!

        // upper row
        val upperRow = buildRow(true, foregroundSection, backgroundSection)

        // bottom row
        val bottomRow =
            buildRow(true, boldSection, italicSection, underlineSection)

        BoxLayout(BoxLayout.Y_AXIS) {
            +upperRow
            verticalStrut(10)
            +bottomRow
        }
    }
}

fun buildRow(addMargins: Boolean, vararg sections: JComponent) = JPanel {
    BoxLayout(BoxLayout.X_AXIS) {
        if (addMargins) horizontalGlue()
        sections
            .intersperseWith { Box.createHorizontalGlue() }
            .forEach { +(it) }
        if (addMargins) horizontalGlue()
    }
}
