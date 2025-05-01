package rars.venus.run

import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import java.awt.event.ActionEvent
import javax.swing.KeyStroke

/**
 * Action class for the Run menu item to clear execution breakpoints that have
 * been set. It is a listener and is notified whenever a breakpoint is added or
 * removed, thus will set its enabled status true or false depending on whether
 * breakpoints remain after that action.
 */
class RunClearBreakpointsAction(
    mnemonic: Int, accel: KeyStroke, gui: VenusUI
) : GuiAction(
    "Clear all breakpoints",
    "Clears all execution breakpoints set since the last assemble.",
    null,
    mnemonic,
    accel,
    gui
) {
    init {
        val textSegment = mainUI.mainPane.executePane.textSegment
        textSegment.registerTableModelListener {
            this.isEnabled = textSegment.getBreakpointCount() > 0
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        mainUI.mainPane.executePane.textSegment.clearAllBreakpoints()
    }
}
