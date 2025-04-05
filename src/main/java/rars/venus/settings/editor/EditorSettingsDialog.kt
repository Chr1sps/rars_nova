package rars.venus.settings.editor

import rars.settings.AllSettings
import rars.venus.VenusUI
import rars.venus.settings.closeDialog
import rars.venus.settings.editor.controllers.EditorSettingsController
import rars.venus.settings.editor.views.PanelWithTextAreaView
import rars.venus.settings.editor.views.PickerCardView
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog

class EditorSettingsDialog(
    mainUI: VenusUI,
    allSettings: AllSettings
) : JDialog(mainUI, "Editor Settings", true) {
    init {
        val (
            boolSettings,
            fontSettings,
            editorThemeSettings,
            highlightingSettings,
            otherSettings,
        ) = allSettings
        val pickerCardView = PickerCardView(
            editorThemeSettings.currentTheme,
            otherSettings.caretBlinkRate,
            otherSettings.editorTabSize
        )
        val panelWithTextAreaView = PanelWithTextAreaView(pickerCardView, allSettings)
        val treePanel = TreePanel()
        val mainPanel = EditorSettingsPanel(treePanel, panelWithTextAreaView)
        EditorSettingsController(
            mainPanel,
            this,
            treePanel,
            fontSettings,
            editorThemeSettings,
            otherSettings,
        )
        contentPane = mainPanel
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        pack()
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(we: WindowEvent?) = closeDialog()
        })
        setLocationRelativeTo(mainUI)
    }
}
