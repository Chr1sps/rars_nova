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
        val outerComponent = this.textArea.outerComponent.apply {
            val textAreaSize = Dimension(500, 300)
            minimumSize = textAreaSize
            preferredSize = textAreaSize
            maximumSize = textAreaSize
        }
        this.BoxLayout(BoxLayout.Y_AXIS) {
            +pickerCardView
            verticalGlue()
            +outerComponent
        }
    }

    companion object {
        private fun createTextArea(
            allSettings: AllSettings
        ): TextEditingArea {
            val (_, fontSettings, _, _, otherSettings) = allSettings
            val exampleText = """
            # Some macro definitions to print strings
            string:
            ${'\t'}.asciz "Some string"
            char:
            ${'\t'}.byte 'a'
            .macro printStr (%str) # print a string
            ${'\t'}.data
            myLabel:
            ${'\t'}.asciz %str
            ${'\t'}.text
            ${'\t'}li a7, 4
            ${'\t'}la a0, myLabel
            ${'\t'}ecall
            .end_macro
            """.trimIndent()
            val selectedText = "myLabel:"
            val selectionStart = exampleText.indexOf(selectedText)
            val selectionEnd = selectionStart + selectedText.length
            return createTextEditingArea(allSettings).apply {
                this.text = exampleText
                this.tabSize = otherSettings.editorTabSize
                this.caretBlinkRate = otherSettings.caretBlinkRate
                this.font = fontSettings.currentFont
                select(selectionStart, selectionEnd)
            }
        }
    }
}