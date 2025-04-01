package rars.venus

import com.formdev.flatlaf.FlatClientProperties
import rars.settings.AllSettings
import rars.venus.registers.ControlAndStatusWindow
import rars.venus.registers.FloatingPointWindow
import rars.venus.registers.RegistersWindow
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * Creates the tabbed areas in the UI and also created the internal windows that
 * exist in them.
 *
 * @author Sanderson and Bumgarner
 */
class MainPane(
    mainUI: VenusUI,
    editor: Editor,
    regs: RegistersWindow,
    cop1Regs: FloatingPointWindow,
    cop0Regs: ControlAndStatusWindow,
    allSettings: AllSettings,
) : JTabbedPane() {
    @JvmField
    val executePane: ExecutePane

    @JvmField
    val editTabbedPane: EditTabbedPane

    init {
        tabPlacement = SwingConstants.TOP
        tabLayoutPolicy = SCROLL_TAB_LAYOUT
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, false)
        /*
         * Listener has one specific purpose: when Execute tab is selected for the
         * first time, set the bounds of its internal frames by invoking the
         * setWindowsBounds() method. Once this occurs, listener removes itself!
         * We do NOT want to reset bounds each time Execute tab is selected.
         * See ExecutePane.setWindowsBounds documentation for more details.
         */
        addChangeListener(
            object : ChangeListener {
                override fun stateChanged(ce: ChangeEvent) {
                    val tabbedPane = ce.source as JTabbedPane
                    val index = tabbedPane.selectedIndex
                    val c = tabbedPane.getComponentAt(index)
                    val executePane = this@MainPane.executePane
                    if (c === executePane) {
                        executePane.setWindowBounds()
                        this@MainPane.removeChangeListener(this)
                    }
                }
            }
        )

        editTabbedPane = EditTabbedPane(mainUI, editor, this, allSettings)
        addTab("Edit", null, editTabbedPane, "Text editor for composing RISCV programs.")

        executePane = ExecutePane(mainUI, regs, cop1Regs, cop0Regs, allSettings)
        addTab(
            "Execute",
            null,
            executePane,
            "View and control assembly language program execution.  Enabled upon successful assemble."
        )
    }

    /**
     * Current edit pane. Implementation changed for MARS 4.0 support
     * for multiple panes, but specification is same.
     */
    val currentEditTabPane: EditPane? get() = this.editTabbedPane.currentEditTab
}