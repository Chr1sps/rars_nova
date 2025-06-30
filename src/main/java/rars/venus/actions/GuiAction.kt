package rars.venus.actions

import rars.venus.VenusUI
import javax.swing.AbstractAction
import javax.swing.Icon
import javax.swing.KeyStroke

/**
 * Parent class for Action subclasses to be defined for every
 * menu/toolbar option.
 */
abstract class GuiAction @JvmOverloads protected constructor(
    name: String,
    description: String? = null,
    icon: Icon? = null,
    /**
     * When a dropdown menu is open, pressing this key will
     * trigger this action, akin to clicking on it's correspoding
     * menu item.
     */
    menuMnemonic: Int? = null,
    /**
     * A global shortcut that can be used to trigger this action
     * at any place in the editor.
     */
    globalShortcut: KeyStroke? = null,
    @JvmField protected val mainUI: VenusUI,
) : AbstractAction(name, icon) {
    init {
        putValue(SHORT_DESCRIPTION, description)
        putValue(MNEMONIC_KEY, menuMnemonic)
        putValue(ACCELERATOR_KEY, globalShortcut)
    }

    companion object {
        const val APPLY_TOOL_TIP_TEXT: String =
            "Apply current settings now and leave dialog open"
        const val RESET_TOOL_TIP_TEXT: String =
            "Reset to initial settings without applying"
        const val CANCEL_TOOL_TIP_TEXT: String =
            "Close dialog without applying current settings"
        const val CLOSE_TOOL_TIP_TEXT: String =
            "Apply current settings and close dialog"
    }
}
