package io.github.chr1sps.rars.venus.settings;

import javax.swing.*;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.venus.GuiAction;

import java.awt.event.ActionEvent;

/*
Copyright (c) 20017,  Benjamin Landers

Developed by Benjamin Landers

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
 * Simple wrapper for boolean settings actions
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SettingsAction extends GuiAction {
    private Settings.Bool setting;

    /**
     * <p>Constructor for SettingsAction.</p>
     *
     * @param name    a {@link java.lang.String} object
     * @param descrip a {@link java.lang.String} object
     * @param setting a {@link io.github.chr1sps.rars.Settings.Bool} object
     */
    public SettingsAction(String name, String descrip, Settings.Bool setting) {
        super(name, null, descrip, null, null);
        this.setting = setting;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        boolean value = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Globals.getSettings().setBooleanSetting(setting, value);
        handler(value);
    }

    /**
     * <p>handler.</p>
     *
     * @param value a boolean
     */
    public void handler(boolean value) {
    }

}
