package rars.venus.actions

import rars.venus.VenusUI
import javax.swing.AbstractAction
import javax.swing.Icon
import javax.swing.KeyStroke

/**
 * Parent class for Action subclasses to be defined for every
 * menu/toolbar option.
 */
abstract class GuiAction protected constructor(
    name: String, icon: Icon?, description: String,
    mnemonic: Int?, accel: KeyStroke?,
    @JvmField protected val mainUI: VenusUI
) : AbstractAction(name, icon) {
    init {
        putValue(SHORT_DESCRIPTION, description)
        putValue(MNEMONIC_KEY, mnemonic)
        putValue(ACCELERATOR_KEY, accel)
    }

    companion object {
        const val APPLY_TOOL_TIP_TEXT: String = "Apply current settings now and leave dialog open"
        const val RESET_TOOL_TIP_TEXT: String = "Reset to initial settings without applying"
        const val CANCEL_TOOL_TIP_TEXT: String = "Close dialog without applying current settings"
        const val CLOSE_TOOL_TIP_TEXT: String = "Apply current settings and close dialog"
    }
}
