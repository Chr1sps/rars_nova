package rars.venus.settings.editor.views

import rars.settings.AllSettings
import rars.venus.editors.TextEditingArea
import rars.venus.editors.createTextEditingArea
import rars.venus.util.BoxLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel

class PanelWithTextAreaView(
    val pickerCardView: PickerCardView,
    allSettings: AllSettings,
) : JPanel() {
    val textArea: TextEditingArea = createTextArea(allSettings)

    init {
        BoxLayout(BoxLayout.Y_AXIS) {
            +pickerCardView
            verticalGlue()
            +textArea.outerComponent.apply {
                val textAreaSize = Dimension(500, 300)
                minimumSize = textAreaSize
                preferredSize = textAreaSize
                maximumSize = textAreaSize
            }
        }
    }
}

private fun createTextArea(
    allSettings: AllSettings
): TextEditingArea {
    val tab = '\t'
    val myLabel = "myLabel:"
    val exampleText = """
        # Some macro definitions to print strings
        string:
        $tab.asciz "Some string"
        char:
        $tab.byte 'a'
        .macro printStr (%str) # print a string
        $tab.data
        $myLabel
        $tab.asciz %str
        $tab.text
        ${tab}li a7, 4
        ${tab}la a0, myLabel
        ${tab}ecall
        .end_macro
    """.trimIndent()
    val selectionStart = exampleText.indexOf(myLabel)
    val selectionEnd = selectionStart + myLabel.length
    val fontSettings = allSettings.fontSettings
    val otherSettings = allSettings.otherSettings
    return createTextEditingArea(allSettings).apply {
        text = exampleText
        tabSize = otherSettings.editorTabSize
        caretBlinkRate = otherSettings.caretBlinkRate
        font = fontSettings.currentFont
        select(selectionStart, selectionEnd)
    }
}
