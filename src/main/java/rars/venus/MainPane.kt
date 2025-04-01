package rars.venus;

import org.jetbrains.annotations.NotNull;
import rars.venus.registers.ControlAndStatusWindow;
import rars.venus.registers.FloatingPointWindow;
import rars.venus.registers.RegistersWindow;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_CLOSABLE;

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
 * Creates the tabbed areas in the UI and also created the internal windows that
 * exist in them.
 *
 * @author Sanderson and Bumgarner
 */
public final class MainPane extends JTabbedPane {
    public final @NotNull ExecutePane executePane;
    public final @NotNull EditTabbedPane editTabbedPane;

    public MainPane(
        final @NotNull VenusUI mainUI, final Editor editor, final RegistersWindow regs,
        final FloatingPointWindow cop1Regs, final ControlAndStatusWindow cop0Regs
    ) {
        super();

        this.setTabPlacement(JTabbedPane.TOP);
        this.editTabbedPane = new EditTabbedPane(mainUI, editor, this);
        this.executePane = new ExecutePane(mainUI, regs, cop1Regs, cop0Regs);

        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        final String editTabTitle = "Edit";
        this.addTab(editTabTitle, null, this.editTabbedPane);

        final String executeTabTitle = "Execute";
        this.addTab(executeTabTitle, null, this.executePane);

        this.setToolTipTextAt(0, "Text editor for composing RISCV programs.");
        this.setToolTipTextAt(
            1,
            "View and control assembly language program execution.  Enabled upon successful assemble."
        );

        /*
         * Listener has one specific purpose: when Execute tab is selected for the
         * first time, set the bounds of its internal frames by invoking the
         * setWindowsBounds() method. Once this occurs, listener removes itself!
         * We do NOT want to reset bounds each time Execute tab is selected.
         * See ExecutePane.setWindowsBounds documentation for more details.
         */
        this.addChangeListener(
            new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent ce) {
                    final JTabbedPane tabbedPane = (JTabbedPane) ce.getSource();
                    final int index = tabbedPane.getSelectedIndex();
                    final Component c = tabbedPane.getComponentAt(index);
                    final ExecutePane executePane = MainPane.this.executePane;
                    if (c == executePane) {
                        executePane.setWindowBounds();
                        MainPane.this.removeChangeListener(this);
                    }
                }
            });

        this.putClientProperty(TABBED_PANE_TAB_CLOSABLE, false);
    }

    /**
     * Returns current edit pane. Implementation changed for MARS 4.0 support
     * for multiple panes, but specification is same.
     *
     * @return the editor pane
     */
    public EditPane getEditPane() {
        return this.editTabbedPane.getCurrentEditTab();
    }
}
