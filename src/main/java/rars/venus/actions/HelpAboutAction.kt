package rars.venus.actions

import rars.Globals
import rars.venus.VenusUI
import java.awt.event.ActionEvent
import javax.swing.ImageIcon
import javax.swing.JOptionPane

/**
 * Action for the Help -> About menu item
 */
class HelpAboutAction(gui: VenusUI) : GuiAction(
    "About RARS", null, "Information about RARS",
    null, null, gui
) {
    override fun actionPerformed(e: ActionEvent?) {
        val message = """
            RARS ${Globals.VERSION}    Copyright ${Globals.COPYRIGHT_YEARS}
            RARS is the RISC-V Assembler and Runtime Simulator.
            
            Toolbar and menu icons are from:
              *  Tango Desktop Project (tango.freedesktop.org),
              *  glyFX (www.glyfx.com) Common Toolbar Set,
              *  KDE-Look (www.kde-look.org) crystalline-blue-0.1,
              *  Icon-King (www.icon-king.com) Nuvola 1.0.
        """.trimIndent()
        JOptionPane.showMessageDialog(
            mainUI,
            message,
            "About RARS",
            JOptionPane.INFORMATION_MESSAGE,
            ImageIcon("images/RISC-V.png")
        )
    }
}