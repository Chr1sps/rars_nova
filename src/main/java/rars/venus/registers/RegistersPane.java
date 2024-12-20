package rars.venus.registers;

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
 * Contains tabbed areas in the UI to display register contents
 *
 * @author Sanderson
 * @version August 2005
 */
public class RegistersPane extends JTabbedPane {
    private final RegistersWindow regsTab;
    private final FloatingPointWindow fpTab;
    private final ControlAndStatusWindow csrTab;

    /**
     * Constructor for the RegistersPane class.
     *
     * @param regs     a {@link RegistersWindow} object
     * @param cop1     a {@link FloatingPointWindow} object
     * @param cop0     a {@link ControlAndStatusWindow} object
     */
    public RegistersPane(final RegistersWindow regs, final FloatingPointWindow cop1,
                         final ControlAndStatusWindow cop0) {
        super();

        this.regsTab = regs;
        this.fpTab = cop1;
        this.csrTab = cop0;
        this.regsTab.setVisible(true);
        this.fpTab.setVisible(true);
        this.csrTab.setVisible(true);

        this.addTab("Registers", this.regsTab);
        this.addTab("Floating Point", this.fpTab);
        this.addTab("Control and Status", this.csrTab);

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
        return this.regsTab;
    }

    /**
     * Return component containing floating point register set.
     *
     * @return floating point register window
     */
    public FloatingPointWindow getFloatingPointWindow() {
        return this.fpTab;
    }

    /**
     * Return component containing Control and Status register set.
     *
     * @return exceptions register window
     */
    public ControlAndStatusWindow getControlAndStatusWindow() {
        return this.csrTab;
    }

    @Override
    public Dimension getPreferredSize() {
        final var size = super.getPreferredSize();
        int preferredWidth = 0;
        for (int i = 0; i < getTabCount(); i++) {
            final var component = getComponentAt(i);
            preferredWidth = Math.max(preferredWidth, component.getPreferredSize().width);
        }
        return new Dimension(preferredWidth, size.height);
    }
}
