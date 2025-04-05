package rars.venus.registers

import java.awt.Dimension
import javax.swing.JTabbedPane

/**
 * Contains tabbed areas in the UI to display register contents
 *
 * @author Sanderson
 * @version August 2005
 */
class RegistersPane(
    val registersWindow: RegistersWindow,
    val floatingPointWindow: FloatingPointWindow,
    val controlAndStatusWindow: ControlAndStatusWindow
) : JTabbedPane() {
    init {
        registersWindow.isVisible = true
        floatingPointWindow.isVisible = true
        controlAndStatusWindow.isVisible = true

        addTab("Registers", null, registersWindow, "CPU registers")
        addTab("Floating Point", null, floatingPointWindow, "Floating point unit registers")
        addTab("Control and Status", null, controlAndStatusWindow, "Control and Status registers")
    }

    override fun getPreferredSize(): Dimension {
        val preferredWidth = components.maxOf { it.preferredSize.width }
        return Dimension(preferredWidth + 1, super.preferredSize.height)
    }
}
