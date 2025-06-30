package rars.venus

import rars.tools.*
import rars.venus.actions.ToolAction
import java.awt.event.KeyEvent
import javax.swing.JMenu

/**
 * This class provides functionality to bring external Mars tools into the Mars
 * system by adding them to its Tools menu. This permits anyone with knowledge
 * of the Mars public interfaces, in particular of the Memory and Register
 * classes, to write applications which can interact with a MIPS program
 * executing under Mars. The execution is of course simulated. The
 * private method for loading tool classes is adapted from Bret Barker's
 * GameServer class from the book "Developing Games In Java".
 *
 * @author Pete Sanderson with help from Bret Barker
 * @version August 2005
 */
object ToolLoader {
    private const val TOOLS_MENU_NAME = "Tools"

    /**
     * List of functions that produce tools given the main UI.
     */
    private val TOOL_PRODUCERS: List<(VenusUI) -> AbstractTool> = listOf(
        ::BHTSimulator,
        ::CacheSimulator,
        ::DigitalLabSim,
        ::FloatRepresentation,
        ::InstructionCounter,
        ::InstructionMemoryDump,
        ::InstructionStatistics,
        ::KeyboardAndDisplaySimulator,
        ::MemoryReferenceVisualization,
        ::TimerTool
    )

    /**
     * Called in VenusUI to build its Tools menu. If there are no qualifying tools
     * or any problems accessing those tools, it returns null. A qualifying tool
     * must be a class in the Tools package that implements Tool, must be compiled
     * into a .class file, and its .class file must be in the same Tools folder as
     * Tool.class.
     *
     * @return a Tools JMenu if qualifying tool classes are found, otherwise null
     */
    @JvmStatic
    fun buildToolsMenu(mainUI: VenusUI): JMenu = JMenu(TOOLS_MENU_NAME).apply {
        setMnemonic(KeyEvent.VK_T)
        for (toolProducer in TOOL_PRODUCERS) {
            add(ToolAction(toolProducer(mainUI)))
        }
    }
}
