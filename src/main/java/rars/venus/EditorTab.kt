package rars.venus

import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.venus.editors.createTextEditingArea
import rars.venus.util.BorderLayout
import rars.venus.util.JPanel
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class EditorTab(
    private val mainUI: VenusUI,
    allSettings: AllSettings,
    var fileStatus: FileStatus,
) : JPanel() {
    private val caretPositionLabel = JLabel().apply {
        toolTipText = "Tracks the current position of the text editing cursor."
    }

    val textArea = createTextEditingArea(allSettings).apply {
        val (
            boolSettings,
            fontSettings,
            editorThemeSettings,
            highlightingSettings,
            otherSettings
        ) = allSettings

        theme = editorThemeSettings.currentTheme.toEditorTheme()
        editorThemeSettings.onChangeListenerHook.subscribe {
            theme = editorThemeSettings.currentTheme.toEditorTheme()
        }

        font = fontSettings.currentFont
        fontSettings.onChangeListenerHook.subscribe {
            font = fontSettings.currentFont
        }

        lineHighlightEnabled =
            boolSettings.getSetting(BoolSetting.EDITOR_CURRENT_LINE_HIGHLIGHTING)
        boolSettings.onChangeListenerHook.subscribe {
            lineHighlightEnabled =
                boolSettings.getSetting(BoolSetting.EDITOR_CURRENT_LINE_HIGHLIGHTING)
        }

        caretBlinkRate = otherSettings.caretBlinkRate
        tabSize = otherSettings.editorTabSize
        otherSettings.onChangeListenerHook.subscribe {
            caretBlinkRate = otherSettings.caretBlinkRate
            tabSize = otherSettings.editorTabSize
        }

        caret.addChangeListener {
            val (line, column) = caretPosition
            displayCaretPosition(Pair(line + 1, column + 1))
        }

        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                when (fileStatus) {
                    is FileStatus.New.NotEdited -> {
                        val newFileStatus =
                            FileStatus.New.Edited((fileStatus as FileStatus.New.NotEdited).tmpName)
                        fileStatus = newFileStatus
                        mainUI.mainPane.editTabbedPane.setTitleForTab(
                            this@EditorTab,
                            newFileStatus
                        )
                    }
                    is FileStatus.Existing.NotEdited -> {
                        val newFileStatus = FileStatus.Existing.Edited(
                            (fileStatus as FileStatus.Existing.NotEdited).file
                        )
                        fileStatus = newFileStatus
                        mainUI.mainPane.editTabbedPane.setTitleForTab(
                            this@EditorTab,
                            newFileStatus
                        )
                    }
                    else -> Unit
                }
                /*
                        switch (FileStatusOld.getSystemState()) {
                            case FileStatusOld.State.NEW_NOT_EDITED ->
                                FileStatusOld.setSystemState(FileStatusOld.State.NEW_EDITED);
                            case FileStatusOld.State.NEW_EDITED -> {
                            }
                            default ->
                                FileStatusOld.setSystemState(FileStatusOld.State.EDITED);
                        }
                 */
                mainUI.mainPane.executePane.clearPane()
            }

            override fun removeUpdate(e: DocumentEvent?) = insertUpdate(e)

            override fun changedUpdate(e: DocumentEvent?) = Unit
        })
    }

    init {
        val editInfo = JPanel {
            BorderLayout {
                this[BorderLayout.WEST] = caretPositionLabel
            }
        }
        BorderLayout {
            this[BorderLayout.CENTER] = textArea.outerComponent
            this[BorderLayout.SOUTH] = editInfo
        }
        displayCaretPosition(Pair(0, 0))
    }

    fun displayCaretPosition(position: Pair<Int, Int>) {
        caretPositionLabel.text =
            "line: ${position.first}, column: ${position.second}"
    }
}