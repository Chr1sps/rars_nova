package rars.venus.settings;

import org.jetbrains.annotations.NotNull;
import rars.settings.BoolSetting;
import rars.venus.actions.GuiAction;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static rars.Globals.BOOL_SETTINGS;

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
 */
public final class SettingsAction extends GuiAction {
    private final BoolSetting setting;
    private final Handler handler;

    public SettingsAction(
        final String name, final String descrip, final @NotNull BoolSetting setting, final @NotNull VenusUI mainUI,
        final @NotNull Handler handler
    ) {
        super(name, null, descrip, null, null, mainUI);
        this.setting = setting;
        this.handler = handler;
    }

    public SettingsAction(
        final String name,
        final String descrip,
        final @NotNull BoolSetting setting,
        final @NotNull VenusUI mainUI
    ) {
        this(
            name, descrip, setting, mainUI, (ignored) -> {
            }
        );
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        final boolean value = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        BOOL_SETTINGS.setSettingAndSave(setting, value);
        this.handler.handler(value);
    }

    @FunctionalInterface
    public interface Handler {
        void handler(boolean value);
    }
}
