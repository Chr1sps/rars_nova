package io.github.chr1sps.rars.venus.registers;

import javax.swing.*;

import io.github.chr1sps.rars.venus.VenusUI;

import java.awt.Color;

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
 * Contains tabbed areas in the UI to display register contents
 *
 * @author Sanderson
 * @version August 2005
 */
public class RegistersPane extends JTabbedPane {
    private final RegistersWindow regsTab;
    private final FloatingPointWindow fpTab;
    private final ControlAndStatusWindow csrTab;

    private final VenusUI mainUI;

    /**
     * Constructor for the RegistersPane class.
     *
     * @param appFrame a {@link io.github.chr1sps.rars.venus.VenusUI} object
     * @param regs     a {@link io.github.chr1sps.rars.venus.registers.RegistersWindow} object
     * @param cop1     a {@link io.github.chr1sps.rars.venus.registers.FloatingPointWindow} object
     * @param cop0     a {@link io.github.chr1sps.rars.venus.registers.ControlAndStatusWindow} object
     */
    public RegistersPane(VenusUI appFrame, RegistersWindow regs, FloatingPointWindow cop1,
                         ControlAndStatusWindow cop0) {
        super();
        this.mainUI = appFrame;

        regsTab = regs;
        fpTab = cop1;
        csrTab = cop0;
        regsTab.setVisible(true);
        fpTab.setVisible(true);
        csrTab.setVisible(true);

        this.addTab("Registers", regsTab);
        this.addTab("Floating Point", fpTab);
        this.addTab("Control and Status", csrTab);
        this.setForeground(Color.black);

        this.setToolTipTextAt(0, "CPU registers");
        this.setToolTipTextAt(1, "Floating point unit registers");
        this.setToolTipTextAt(2, "Control and Status registers");
    }

    /**
     * Return component containing integer register set.
     *
     * @return integer register window
     */
    public RegistersWindow getRegistersWindow() {
        return regsTab;
    }

    /**
     * Return component containing floating point register set.
     *
     * @return floating point register window
     */
    public FloatingPointWindow getFloatingPointWindow() {
        return fpTab;
    }

    /**
     * Return component containing Control and Status register set.
     *
     * @return exceptions register window
     */
    public ControlAndStatusWindow getControlAndStatusWindow() {
        return csrTab;
    }
}
