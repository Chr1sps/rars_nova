package rars.venus

import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.venus.registers.ControlAndStatusWindow
import rars.venus.registers.FloatingPointWindow
import rars.venus.registers.RegistersWindow
import java.awt.Dimension
import javax.swing.JDesktopPane

/**
 * Container for the execution-related windows. Currently displayed as a tabbed
 * pane.
 *
 * @author Sanderson and Team JSpim
 */
class ExecutePane(
    private val mainUI: VenusUI,
    val registerValues: RegistersWindow,
    val fpRegValues: FloatingPointWindow,
    val csrValues: ControlAndStatusWindow,
    allSettings: AllSettings
) : JDesktopPane() {

    val addressDisplayBaseChooser = NumberDisplayBasePicker(
        "Hexadecimal Addresses",
        allSettings.boolSettings.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX),
        this
    ).apply {
        toolTipText = "If checked, displays all memory addresses in hexadecimal. Otherwise, decimal."
    }

    val valueDisplayBaseChooser = NumberDisplayBasePicker(
        "Hexadecimal Values",
        allSettings.boolSettings.getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX),
        this
    ).apply {
        toolTipText = "If checked, displays all memory and register contents in hexadecimal. Otherwise, decimal."
    }

    val dataSegment = DataSegmentWindow(
        arrayOf(addressDisplayBaseChooser, valueDisplayBaseChooser), this, allSettings
    ).apply {
        pack()
        isVisible = true
    }

    val textSegment = TextSegmentWindow(this, allSettings).apply {
        isVisible = true
        pack()
    }
    private var isLabelWindowVisible = allSettings.boolSettings.getSetting(BoolSetting.LABEL_WINDOW_VISIBILITY)
    val labelsWindow = LabelsWindow(this).apply {
        pack()
        isVisible = isLabelWindowVisible
    }

    init {
        add(textSegment)
        add(dataSegment)
        add(labelsWindow)
    }

    /**
     * This method will set the bounds of this JDesktopPane's internal windows
     * relative to the current size of this JDesktopPane. Such an operation
     * cannot be adequately done at constructor time because the actual
     * size of the desktop pane window is not yet established. Layout manager
     * is not a good option here because JDesktopPane does not work well with
     * them (the whole idea of using JDesktopPane with internal frames is to
     * have mini-frames that you can resize, move around, minimize, etc). This
     * method should be invoked only once: the first time the Execute tab is
     * selected (a change listener invokes it). We do not want it invoked
     * on subsequent tab selections; otherwise, user manipulations of the
     * internal frames would be lost the next time execute tab is selected.
     */
    fun setWindowBounds() {
        val fullWidth = this.size.width - this.insets.left - this.insets.right
        val fullHeight = this.size.height - this.insets.top - this.insets.bottom
        val halfHeight = fullHeight / 2
        val textDim = Dimension((fullWidth * 0.75).toInt(), halfHeight)
        val dataDim = Dimension(fullWidth, halfHeight)
        val labelDimension = Dimension((fullWidth * 0.25).toInt(), halfHeight)
        val textFullDim = Dimension(fullWidth, halfHeight)
        this.dataSegment.setBounds(0, textDim.height + 1, dataDim.width, dataDim.height)
        if (isLabelWindowVisible) {
            this.textSegment.setBounds(0, 0, textDim.width, textDim.height)
            this.labelsWindow.setBounds(textDim.width + 1, 0, labelDimension.width, labelDimension.height)
        } else {
            this.textSegment.setBounds(0, 0, textFullDim.width, textFullDim.height)
            this.labelsWindow.setBounds(0, 0, 0, 0)
        }
    }

    /**
     * Show or hide the label window (symbol table). If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     *
     * @param visibility
     * set to true or false
     */
    fun setLabelWindowVisibility(visibility: Boolean) {
        when {
            !visibility && this.isLabelWindowVisible -> {
                isLabelWindowVisible = false
                textSegment.isVisible = false
                labelsWindow.isVisible = false
                setWindowBounds()
                textSegment.isVisible = true
            }
            visibility && !this.isLabelWindowVisible -> {
                isLabelWindowVisible = true
                textSegment.isVisible = false
                setWindowBounds()
                textSegment.isVisible = true
                labelsWindow.isVisible = true
            }
        }
    }

    /**
     * Clears out all components of the Execute tab: text segment
     * display, data segment display, label display and register display.
     * This will typically be done upon File->Close, Open, New.
     */
    fun clearPane() {
        this.textSegment.clearWindow()
        this.dataSegment.clearWindow()
        this.registerValues.clearWindow()
        this.fpRegValues.clearWindow()
        this.csrValues.clearWindow()
        this.labelsWindow.clearWindow()
        // seems to be required, to display cleared Execute tab contents...
        if (this.mainUI.mainPane.selectedComponent === this) {
            this.mainUI.mainPane.selectedComponent = this.mainUI.mainPane.editTabbedPane
            this.mainUI.mainPane.selectedComponent = this
        }
    }

    val valueDisplayFormat get() = valueDisplayBaseChooser.base
    val addressDisplayFormat get() = addressDisplayBaseChooser.base

    /**
     * Update display of columns based on state of given chooser. Normally
     * called only by the chooser's ItemListener.
     *
     * @param chooser
     * the GUI object manipulated by the user to change number base
     */
    fun numberDisplayBaseChanged(chooser: NumberDisplayBasePicker?) {
        if (chooser == this.valueDisplayBaseChooser) {
            // Have all internal windows update their value columns
            this.registerValues.updateRegisters()
            this.fpRegValues.updateRegisters()
            this.csrValues.updateRegisters()
            this.dataSegment.updateValues()
            this.textSegment.updateBasicStatements()
        } else {
            // Have all internal windows update their address columns
            this.dataSegment.updateDataAddresses()
            this.labelsWindow.updateLabelAddresses()
            this.textSegment.updateCodeAddresses()
            this.textSegment.updateBasicStatements()
        }
    }
}
