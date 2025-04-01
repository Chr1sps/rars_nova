package rars.venus.actions

import rars.tools.AbstractTool
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

/**
 * Connects a Tool class (class that implements Tool interface) to
 * the Mars menu system by supplying the response to that tool's menu item
 * selection.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
class ToolAction(private val tool: AbstractTool) : AbstractAction(tool.name, null) {
    override fun actionPerformed(e: ActionEvent?) {
        this.tool.action()
    }
}