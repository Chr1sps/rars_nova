package rars.venus;

import org.jetbrains.annotations.NotNull;
import rars.settings.AllSettings;
import rars.settings.BoolSetting;
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
public final class ExecutePane extends JDesktopPane {
    public final @NotNull RegistersWindow registerValues;
    public final @NotNull FloatingPointWindow fpRegValues;
    public final @NotNull ControlAndStatusWindow csrValues;
    public final @NotNull DataSegmentWindow dataSegment;
    public final @NotNull TextSegmentWindow textSegment;
    public final @NotNull LabelsWindow labelValues;
    public final @NotNull NumberDisplayBaseChooser valueDisplayBase;
    public final @NotNull NumberDisplayBaseChooser addressDisplayBase;
    private final @NotNull VenusUI mainUI;
    private boolean labelWindowVisible;

    /**
     * initialize the Execute pane with major components
     *
     * @param mainUI
     *     the parent GUI
     * @param regs
     *     window containing integer register set
     * @param fpRegs
     *     window containing floating point register set
     * @param csrRegs
     *     window containing the CSR set
     */
    public ExecutePane(
        final @NotNull VenusUI mainUI,
        final @NotNull RegistersWindow regs,
        final @NotNull FloatingPointWindow fpRegs,
        final @NotNull ControlAndStatusWindow csrRegs,
        final @NotNull AllSettings allSettings
    ) {
        super();
        this.mainUI = mainUI;
        // Although these are displayed in Data Segment, they apply to all three
        // internal
        // windows within the Execute pane. So they will be housed here.
        final var boolSettings = allSettings.boolSettings;
        this.addressDisplayBase = new NumberDisplayBaseChooser(
            "Hexadecimal Addresses",
            boolSettings.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX),
            this
        );
        this.valueDisplayBase = new NumberDisplayBaseChooser(
            "Hexadecimal Values",
            boolSettings.getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX),
            this
        );
        this.addressDisplayBase
            .setToolTipText("If checked, displays all memory addresses in hexadecimal.  Otherwise, decimal.");
        this.valueDisplayBase.setToolTipText(
            "If checked, displays all memory and register contents in hexadecimal.  Otherwise, decimal.");
        final NumberDisplayBaseChooser[] choosers = {this.addressDisplayBase, this.valueDisplayBase};
        this.registerValues = regs;
        this.fpRegValues = fpRegs;
        this.csrValues = csrRegs;
        this.textSegment = new TextSegmentWindow(this, allSettings);
        this.dataSegment = new DataSegmentWindow(
            choosers, this, allSettings
        );
        this.labelValues = new LabelsWindow(this);
        this.labelWindowVisible = boolSettings.getSetting(BoolSetting.LABEL_WINDOW_VISIBILITY);
        this.add(this.textSegment);
        this.add(this.dataSegment);
        this.add(this.labelValues);
        this.textSegment.pack();
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
        final Dimension textDim = new Dimension((int) (fullWidth * 0.75), halfHeight);
        final Dimension dataDim = new Dimension(fullWidth, halfHeight);
        final Dimension labelDimension = new Dimension((int) (fullWidth * 0.25), halfHeight);
        final Dimension textFullDim = new Dimension(fullWidth, halfHeight);
        this.dataSegment.setBounds(0, textDim.height + 1, dataDim.width, dataDim.height);
        if (this.labelWindowVisible) {
            this.textSegment.setBounds(0, 0, textDim.width, textDim.height);
            this.labelValues.setBounds(textDim.width + 1, 0, labelDimension.width, labelDimension.height);
        } else {
            this.textSegment.setBounds(0, 0, textFullDim.width, textFullDim.height);
            this.labelValues.setBounds(0, 0, 0, 0);
        }
    }

    /**
     * Show or hide the label window (symbol table). If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     *
     * @param visibility
     *     set to true or false
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
        this.textSegment.clearWindow();
        this.dataSegment.clearWindow();
        this.registerValues.clearWindow();
        this.fpRegValues.clearWindow();
        this.csrValues.clearWindow();
        this.labelValues.clearWindow();
        // seems to be required, to display cleared Execute tab contents...
        if (this.mainUI.mainPane.getSelectedComponent() == this) {
            this.mainUI.mainPane.setSelectedComponent(this.mainUI.mainPane.editTabbedPane);
            this.mainUI.mainPane.setSelectedComponent(this);
        }
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
     * Update display of columns based on state of given chooser. Normally
     * called only by the chooser's ItemListener.
     *
     * @param chooser
     *     the GUI object manipulated by the user to change number base
     */
    public void numberDisplayBaseChanged(final NumberDisplayBaseChooser chooser) {
        if (chooser == this.valueDisplayBase) {
            // Have all internal windows update their value columns
            this.registerValues.updateRegisters();
            this.fpRegValues.updateRegisters();
            this.csrValues.updateRegisters();
            this.dataSegment.updateValues();
            this.textSegment.updateBasicStatements();
        } else {
            // Have all internal windows update their address columns
            this.dataSegment.updateDataAddresses();
            this.labelValues.updateLabelAddresses();
            this.textSegment.updateCodeAddresses();
            this.textSegment.updateBasicStatements();
        }
    }

}
