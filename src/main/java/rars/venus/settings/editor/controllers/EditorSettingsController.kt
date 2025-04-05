package rars.venus.settings.editor.controllers

import rars.settings.EditorThemeSettingsImpl
import rars.settings.FontSettingsImpl
import rars.settings.OtherSettingsImpl
import rars.settings.SettingsTheme
import rars.venus.settings.editor.EditorSettingsDialog
import rars.venus.settings.editor.EditorSettingsPanel
import rars.venus.settings.editor.TreeNodeData
import rars.venus.settings.editor.TreePanel
import javax.swing.tree.DefaultMutableTreeNode

class EditorSettingsController(
    editorSettingsView: EditorSettingsPanel,
    dialog: EditorSettingsDialog,
    treePanel: TreePanel,
    fontSettings: FontSettingsImpl,
    private val editorThemeSettings: EditorThemeSettingsImpl,
    otherSettings: OtherSettingsImpl
) {
    private val fontSettingsController: FontSettingsController
    private val baseStyleSettingsController: BaseStyleSettingsController
    private val syntaxStyleSettingsController: SyntaxStyleSettingsController
    private val otherSettingsController: OtherSettingsController
    var settingsTheme: SettingsTheme

    init {
        this.settingsTheme = editorThemeSettings.currentTheme
        val pickerCardView = editorSettingsView.panelWithTextAreaView.pickerCardView
        val textArea = editorSettingsView.panelWithTextAreaView.textArea

        this.fontSettingsController = FontSettingsController(
            pickerCardView.fontSettingsView,
            textArea,
            fontSettings
        )
        PresetsController(
            pickerCardView.presetsView,
            textArea,
            this
        )
        this.baseStyleSettingsController = BaseStyleSettingsController(
            pickerCardView.baseStyleView,
            textArea,
            this
        )
        this.syntaxStyleSettingsController = SyntaxStyleSettingsController(
            pickerCardView.syntaxStyleView,
            this,
            textArea
        )
        this.otherSettingsController = OtherSettingsController(
            pickerCardView.otherSettingsView,
            textArea,
            otherSettings
        )
        editorSettingsView.bottomRowComponent.apply {
            applyButton.addActionListener { applySettings() }
            applyAndCloseButton.addActionListener {
                applySettings()
                dialog.isVisible = false
                dialog.dispose()
            }
            cancelButton.addActionListener {
                discardSettings()
                dialog.isVisible = false
                dialog.dispose()
            }
        }
        treePanel.tree.addTreeSelectionListener {
            val selectedNode = treePanel.tree.lastSelectedPathComponent
                as? DefaultMutableTreeNode
                ?: return@addTreeSelectionListener
            when (val node: Any? = selectedNode.getUserObject()) {
                is TreeNodeData.Syntax -> {
                    syntaxStyleSettingsController.setCurrentKey(node.type)
                    pickerCardView.showSyntaxStyleView()
                }

                treePanel.fontSettingsNode -> pickerCardView.showFontView()
                treePanel.generalSchemeSettingsNode -> pickerCardView.showBaseStyleView()
                treePanel.otherSettingsNode -> pickerCardView.showOtherSettings()
                treePanel.presetsNode -> pickerCardView.showPresets()
                else -> pickerCardView.showEmpty()
            }
        }
    }

    private fun discardSettings() {
        this.settingsTheme = this.editorThemeSettings.currentTheme
        this.fontSettingsController.resetButtonValues()
        this.baseStyleSettingsController.resetButtonValues()
        this.syntaxStyleSettingsController.resetButtonValues()
    }

    fun updateThemeControllers() {
        this.fontSettingsController.resetButtonValues()
        this.baseStyleSettingsController.resetButtonValues()
    }

    private fun applySettings() {
        this.fontSettingsController.applySettings()

        this.editorThemeSettings.currentTheme = this.settingsTheme
        this.editorThemeSettings.saveSettingsToPreferences()

        this.otherSettingsController.applySettings()
    }
}
