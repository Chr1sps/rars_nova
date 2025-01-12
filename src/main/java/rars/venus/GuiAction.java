package rars.venus;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
 * parent class for Action subclasses to be defined for every menu/toolbar
 * option.
 */
public class GuiAction extends AbstractAction {
    public static final String APPLY_TOOL_TIP_TEXT = "Apply current settings now and leave dialog open";
    public static final String RESET_TOOL_TIP_TEXT = "Reset to initial settings without applying";
    public static final String CANCEL_TOOL_TIP_TEXT = "Close dialog without applying current settings";
    protected static final String CLOSE_TOOL_TIP_TEXT = "Apply current settings and close dialog";
    // NOTE: These must follow same sequence and buttons must
    // follow this sequence too!
    private static final int gridVGap = 2;
    private static final int gridHGap = 2;
    // Tool tips for color buttons
    private static final String SAMPLE_TOOL_TIP_TEXT = "Preview based on background and text color settings";
    private static final String BACKGROUND_TOOL_TIP_TEXT = "Click, to select background color";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
    private static final String FONT_TOOL_TIP_TEXT = "Click, to select text font";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select default color (disables color select " +
        "buttons)";
    // Tool tips for the data and register highlighting enable/disable controls
    private static final String DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in " +
        "Data Segment window";
    private static final String REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting " +
        "in Register windows";
    private static final String fontButtonText = "font";
    protected final @NotNull VenusUI mainUI;

    protected GuiAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final @NotNull VenusUI mainUI
    ) {
        super(name, icon);
        this.mainUI = mainUI;
        putValue(SHORT_DESCRIPTION, descrip);
        putValue(MNEMONIC_KEY, mnemonic);
        putValue(ACCELERATOR_KEY, accel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * does nothing by default. Should be over-ridden by subclass
     */
    @Override
    public void actionPerformed(final ActionEvent e) {

    }
}
