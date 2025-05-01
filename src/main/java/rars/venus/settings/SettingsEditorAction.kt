package rars.venus.settings

import rars.settings.AllSettings
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import rars.venus.settings.editor.EditorSettingsDialog
import java.awt.event.ActionEvent

/**
 * Action class for the Settings menu item for text editor settings.
 */
class SettingsEditorAction(
    mainUI: VenusUI,
    private val allSettings: AllSettings
) : GuiAction("Editor...", "View and modify text editor settings.", null, null, null, mainUI) {
    override fun actionPerformed(e: ActionEvent?): Unit = EditorSettingsDialog(mainUI, allSettings).run {
        isVisible = true
    }
}
