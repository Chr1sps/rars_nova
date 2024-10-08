package rars.venus;

import rars.Globals;
import rars.Settings;
import rars.venus.registers.ControlAndStatusWindow;
import rars.venus.registers.FloatingPointWindow;
import rars.venus.registers.RegistersWindow;

import javax.swing.*;
import java.awt.*;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Container for the execution-related windows. Currently displayed as a tabbed
 * pane.
 *
 * @author Sanderson and Team JSpim
 */
public class ExecutePane extends JDesktopPane {
    private final RegistersWindow registerValues;
    private final FloatingPointWindow fpRegValues;
    private final ControlAndStatusWindow csrValues;
    private final DataSegmentWindow dataSegment;
    private final TextSegmentWindow textSegment;
    private final LabelsWindow labelValues;
    private final VenusUI mainUI;
    private final NumberDisplayBaseChooser valueDisplayBase;
    private final NumberDisplayBaseChooser addressDisplayBase;
    private boolean labelWindowVisible;

    /**
     * initialize the Execute pane with major components
     *
     * @param mainUI  the parent GUI
     * @param regs    window containing integer register set
     * @param fpRegs  window containing floating point register set
     * @param csrRegs window containing the CSR set
     */
    public ExecutePane(final VenusUI mainUI, final RegistersWindow regs, final FloatingPointWindow fpRegs,
                       final ControlAndStatusWindow csrRegs) {
        this.mainUI = mainUI;
        // Although these are displayed in Data Segment, they apply to all three
        // internal
        // windows within the Execute pane. So they will be housed here.
        this.addressDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Addresses",
                Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_ADDRESSES_IN_HEX));
        this.valueDisplayBase = new NumberDisplayBaseChooser("Hexadecimal Values",
                Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX));// VenusUI.DEFAULT_NUMBER_BASE);
        this.addressDisplayBase
                .setToolTipText("If checked, displays all memory addresses in hexadecimal.  Otherwise, decimal.");
        this.valueDisplayBase.setToolTipText(
                "If checked, displays all memory and register contents in hexadecimal.  Otherwise, decimal.");
        final NumberDisplayBaseChooser[] choosers = {this.addressDisplayBase, this.valueDisplayBase};
        this.registerValues = regs;
        this.fpRegValues = fpRegs;
        this.csrValues = csrRegs;
        this.textSegment = new TextSegmentWindow();
        this.dataSegment = new DataSegmentWindow(choosers);
        this.labelValues = new LabelsWindow();
        this.labelWindowVisible = Globals.getSettings().getBooleanSetting(Settings.Bool.LABEL_WINDOW_VISIBILITY);
        this.add(this.textSegment); // these 3 LOC moved up. DPS 3-Sept-2014
        this.add(this.dataSegment);
        this.add(this.labelValues);
        this.textSegment.pack(); // these 3 LOC added. DPS 3-Sept-2014
        this.dataSegment.pack();
        this.labelValues.pack();
        this.textSegment.setVisible(true);
        this.dataSegment.setVisible(true);
        this.labelValues.setVisible(this.labelWindowVisible);

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
    public void setWindowBounds() {

        final int fullWidth = this.getSize().width - this.getInsets().left - this.getInsets().right;
        final int fullHeight = this.getSize().height - this.getInsets().top - this.getInsets().bottom;
        final int halfHeight = fullHeight / 2;
        final Dimension textDim = new Dimension((int) (fullWidth * .75), halfHeight);
        final Dimension dataDim = new Dimension(fullWidth, halfHeight);
        final Dimension lablDim = new Dimension((int) (fullWidth * .25), halfHeight);
        final Dimension textFullDim = new Dimension(fullWidth, halfHeight);
        this.dataSegment.setBounds(0, textDim.height + 1, dataDim.width, dataDim.height);
        if (this.labelWindowVisible) {
//            System.out.println("YEA");
            this.textSegment.setBounds(0, 0, textDim.width, textDim.height);
            this.labelValues.setBounds(textDim.width + 1, 0, lablDim.width, lablDim.height);
        } else {
            this.textSegment.setBounds(0, 0, textFullDim.width, textFullDim.height);
            this.labelValues.setBounds(0, 0, 0, 0);
        }
    }

    /**
     * Show or hide the label window (symbol table). If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     *
     * @param visibility set to true or false
     */
    public void setLabelWindowVisibility(final boolean visibility) {
        if (!visibility && this.labelWindowVisible) {
            this.labelWindowVisible = false;
            this.textSegment.setVisible(false);
            this.labelValues.setVisible(false);
            this.setWindowBounds();
            this.textSegment.setVisible(true);
        } else if (visibility && !this.labelWindowVisible) {
            this.labelWindowVisible = true;
            this.textSegment.setVisible(false);
            this.setWindowBounds();
            this.textSegment.setVisible(true);
            this.labelValues.setVisible(true);
        }
    }

    /**
     * Clears out all components of the Execute tab: text segment
     * display, data segment display, label display and register display.
     * This will typically be done upon File->Close, Open, New.
     */
    public void clearPane() {
        this.getTextSegmentWindow().clearWindow();
        this.getDataSegmentWindow().clearWindow();
        this.getRegistersWindow().clearWindow();
        this.getFloatingPointWindow().clearWindow();
        this.getControlAndStatusWindow().clearWindow();
        this.getLabelsWindow().clearWindow();
        // seems to be required, to display cleared Execute tab contents...
        if (this.mainUI.getMainPane().getSelectedComponent() == this) {
            this.mainUI.getMainPane().setSelectedComponent(this.mainUI.getMainPane().getEditTabbedPane());
            this.mainUI.getMainPane().setSelectedComponent(this);
        }
    }

    /**
     * Access the text segment window.
     *
     * @return a {@link TextSegmentWindow} object
     */
    public TextSegmentWindow getTextSegmentWindow() {
        return this.textSegment;
    }

    /**
     * Access the data segment window.
     *
     * @return a {@link DataSegmentWindow} object
     */
    public DataSegmentWindow getDataSegmentWindow() {
        return this.dataSegment;
    }

    /**
     * Access the register values window.
     *
     * @return a {@link RegistersWindow} object
     */
    public RegistersWindow getRegistersWindow() {
        return this.registerValues;
    }

    /**
     * Access the floating point values window.
     *
     * @return a {@link FloatingPointWindow} object
     */
    public FloatingPointWindow getFloatingPointWindow() {
        return this.fpRegValues;
    }

    /**
     * Access the Control and Status values window.
     *
     * @return a {@link ControlAndStatusWindow} object
     */
    public ControlAndStatusWindow getControlAndStatusWindow() {
        return this.csrValues;
    }

    /**
     * Access the label values window.
     *
     * @return a {@link LabelsWindow} object
     */
    public LabelsWindow getLabelsWindow() {
        return this.labelValues;
    }

    /**
     * Retrieve the number system base for displaying values (mem/register contents)
     *
     * @return a int
     */
    public int getValueDisplayBase() {
        return this.valueDisplayBase.getBase();
    }

    /**
     * Retrieve the number system base for displaying memory addresses
     *
     * @return a int
     */
    public int getAddressDisplayBase() {
        return this.addressDisplayBase.getBase();
    }

    /**
     * Retrieve component used to set numerical base (10 or 16) of data second
     * display.
     *
     * @return the chooser
     */
    public NumberDisplayBaseChooser getValueDisplayBaseChooser() {
        return this.valueDisplayBase;
    }

    /**
     * Retrieve component used to set numerical base (10 or 16) of address display.
     *
     * @return the chooser
     */
    public NumberDisplayBaseChooser getAddressDisplayBaseChooser() {
        return this.addressDisplayBase;
    }

    /**
     * Update display of columns based on state of given chooser. Normally
     * called only by the chooser's ItemListener.
     *
     * @param chooser the GUI object manipulated by the user to change number base
     */
    public void numberDisplayBaseChanged(final NumberDisplayBaseChooser chooser) {
        if (chooser == this.valueDisplayBase) {
            // Have all internal windows update their second columns
            this.registerValues.updateRegisters();
            this.fpRegValues.updateRegisters();
            this.csrValues.updateRegisters();
            this.dataSegment.updateValues();
            this.textSegment.updateBasicStatements();
        } else { // addressDisplayBase
            // Have all internal windows update their address columns
            this.dataSegment.updateDataAddresses();
            this.labelValues.updateLabelAddresses();
            this.textSegment.updateCodeAddresses();
            this.textSegment.updateBasicStatements();
        }
    }

}
