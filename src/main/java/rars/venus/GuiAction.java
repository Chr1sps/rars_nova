package rars.venus;

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
    /**
     * <p>Constructor for GuiAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     */
    protected GuiAction(final String name, final Icon icon, final String descrip,
                        final Integer mnemonic, final KeyStroke accel) {
        super(name, icon);
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