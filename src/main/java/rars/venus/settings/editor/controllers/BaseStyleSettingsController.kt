package rars.venus.settings.editor.controllers

import rars.venus.editors.TextEditingArea
import rars.venus.settings.editor.views.BaseStyleView

class BaseStyleSettingsController(
    private val view: BaseStyleView,
    textArea: TextEditingArea,
    private val parentController: EditorSettingsController
) {
    init {
        view.apply {
            backgroundColorButton.apply {
                color = parentController.settingsTheme.backgroundColor
                addChangeListener {
                    parentController.settingsTheme.backgroundColor = color
                    textArea.background = parentController.settingsTheme.backgroundColor
                }
            }
            foregroundColorButton.apply {
                color = parentController.settingsTheme.foregroundColor
                addChangeListener {
                    parentController.settingsTheme.foregroundColor = color
                    textArea.foreground = parentController.settingsTheme.foregroundColor
                }
            }
            textSelectionColorButton.apply {
                color = parentController.settingsTheme.selectionColor
                addChangeListener {
                    parentController.settingsTheme.selectionColor = color
                    textArea.selectionColor = parentController.settingsTheme.selectionColor
                }
            }
            caretColorButton.apply {
                color = parentController.settingsTheme.caretColor
                addChangeListener {
                    parentController.settingsTheme.caretColor = color
                    textArea.caretColor = parentController.settingsTheme.caretColor
                }
            }
            lineHighlightColorButton.apply {
                color = parentController.settingsTheme.lineHighlightColor
                addChangeListener {
                    parentController.settingsTheme.lineHighlightColor = color
                    textArea.lineHighlightColor = parentController.settingsTheme.lineHighlightColor
                }
            }
        }
    }

    fun resetButtonValues() {
        val settingsTheme = parentController.settingsTheme
        view.apply {
            foregroundColorButton.color = settingsTheme.foregroundColor
            backgroundColorButton.color = settingsTheme.backgroundColor
            textSelectionColorButton.color = settingsTheme.selectionColor
            caretColorButton.color = settingsTheme.caretColor
            lineHighlightColorButton.color = settingsTheme.lineHighlightColor
        }
    }

}
