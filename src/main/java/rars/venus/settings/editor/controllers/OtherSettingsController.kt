package rars.venus.settings.editor.controllers

import rars.settings.OtherSettingsImpl
import rars.venus.editors.TextEditingArea
import rars.venus.settings.editor.views.OtherSettingsView

class OtherSettingsController(
    view: OtherSettingsView,
    private val textArea: TextEditingArea,
    private val settings: OtherSettingsImpl
) {
    private var caretBlinkRate: Int
    private var editorTabSize: Int

    init {
        caretBlinkRate = settings.caretBlinkRate
        editorTabSize = settings.editorTabSize
        view.apply {
            blinkRateSpinner.apply {
                value = caretBlinkRate
                addChangeListener {
                    caretBlinkRate = value as Int
                    textArea.caretBlinkRate = caretBlinkRate
                }
            }
            tabSizeSpinner.apply {
                value = editorTabSize
                addChangeListener {
                    editorTabSize = value as Int
                    textArea.tabSize = editorTabSize
                }
            }
        }
    }

    fun applySettings() {
        settings.apply {
            setCaretBlinkRateAndSave(caretBlinkRate)
            setEditorTabSizeAndSave(editorTabSize)
        }
    }
}
