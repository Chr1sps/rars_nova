package rars.venus.settings

import rars.settings.BoolSetting
import rars.settings.BoolSettingsImpl
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import java.awt.event.ActionEvent
import javax.swing.JCheckBoxMenuItem

/**
 * Simple wrapper for boolean settings actions
 */
class SettingsAction @JvmOverloads constructor(
    name: String,
    description: String,
    private val boolSettings: BoolSettingsImpl,
    private val setting: BoolSetting,
    mainUI: VenusUI,
    private val handler: Handler = Handler { }
) : GuiAction(name, description, null, null, null, mainUI) {
    override fun actionPerformed(e: ActionEvent) {
        val value = (e.getSource() as JCheckBoxMenuItem).isSelected
        boolSettings.setSettingAndSave(setting, value)
        handler.handler(value)
    }

    fun interface Handler {
        fun handler(value: Boolean)
    }
}
