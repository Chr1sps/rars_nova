package rars.venus.settings;

import rars.Globals;
import rars.settings.BoolSetting;
import rars.venus.GuiAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import static rars.settings.BoolSettings.BOOL_SETTINGS;
import static rars.settings.OtherSettings.OTHER_SETTINGS;

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
 * Action class for the Settings menu item for optionally loading a MIPS
 * exception handler.
 */
public class SettingsExceptionHandlerAction extends GuiAction {

    private JDialog exceptionHandlerDialog;
    private JCheckBox exceptionHandlerSetting;
    private JButton exceptionHandlerSelectionButton;
    private JTextField exceptionHandlerDisplay;

    private boolean initialSelected; // state of check box when dialog initiated.
    private String initialPathname; // selected exception handler when dialog initiated.

    /**
     * <p>Constructor for SettingsExceptionHandlerAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     */
    public SettingsExceptionHandlerAction(final String name, final Icon icon, final String descrip,
                                          final Integer mnemonic, final KeyStroke accel) {
        super(name, icon, descrip, mnemonic, accel);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.initialSelected =
            BOOL_SETTINGS.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED);
        this.initialPathname = OTHER_SETTINGS.getExceptionHandler();
        this.exceptionHandlerDialog = new JDialog(Globals.gui, "Exception Handler", true);
        this.exceptionHandlerDialog.setContentPane(this.buildDialogPanel());
        this.exceptionHandlerDialog.setDefaultCloseOperation(
            JDialog.DO_NOTHING_ON_CLOSE);
        this.exceptionHandlerDialog.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent we) {
                    SettingsExceptionHandlerAction.this.closeDialog();
                }
            });
        this.exceptionHandlerDialog.pack();
        this.exceptionHandlerDialog.setLocationRelativeTo(Globals.gui);
        this.exceptionHandlerDialog.setVisible(true);
    }

    // The dialog box that appears when menu item is selected.
    private JPanel buildDialogPanel() {
        final JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Top row - the check box for setting...
        this.exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
        this.exceptionHandlerSetting
            .setSelected(BOOL_SETTINGS.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED));
        this.exceptionHandlerSetting.addActionListener(new ExceptionHandlerSettingAction());
        contents.add(this.exceptionHandlerSetting, BorderLayout.NORTH);
        // Middle row - the button and text field for exception handler file selection
        final JPanel specifyHandlerFile = new JPanel();
        this.exceptionHandlerSelectionButton = new JButton("Browse");
        this.exceptionHandlerSelectionButton.setEnabled(this.exceptionHandlerSetting.isSelected());
        this.exceptionHandlerSelectionButton.addActionListener(new ExceptionHandlerSelectionAction());
        this.exceptionHandlerDisplay = new JTextField(OTHER_SETTINGS.getExceptionHandler(), 30);
        this.exceptionHandlerDisplay.setEditable(false);
        this.exceptionHandlerDisplay.setEnabled(this.exceptionHandlerSetting.isSelected());
        specifyHandlerFile.add(this.exceptionHandlerSelectionButton);
        specifyHandlerFile.add(this.exceptionHandlerDisplay);
        contents.add(specifyHandlerFile, BorderLayout.CENTER);
        // Bottom row - the control buttons for OK and Cancel
        final Box controlPanel = Box.createHorizontalBox();
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(
            e -> {
                this.performOK();
                this.closeDialog();
            });
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(
            e -> this.closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        contents.add(controlPanel, BorderLayout.SOUTH);
        return contents;
    }

    /// User has clicked "OK" button, so record status of the checkbox and text field.
    private void performOK() {
        final boolean finalSelected = this.exceptionHandlerSetting.isSelected();
        final String finalPathname = this.exceptionHandlerDisplay.getText();
        // If nothing has changed then don't modify setting variables or properties
        // file.
        if (this.initialSelected != finalSelected
            || this.initialPathname == null && finalPathname != null
            || this.initialPathname != null && !this.initialPathname.equals(finalPathname)) {
            BOOL_SETTINGS.setSettingAndSave(BoolSetting.EXCEPTION_HANDLER_ENABLED,
                finalSelected);
            if (finalSelected) {
                OTHER_SETTINGS.setExceptionHandlerAndSave(finalPathname);
            }
        }
    }

    // We're finished with this modal dialog.
    private void closeDialog() {
        this.exceptionHandlerDialog.setVisible(false);
        this.exceptionHandlerDialog.dispose();
    }

    /// Associated action class: exception handler setting. Attached to check box.
    private class ExceptionHandlerSettingAction implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final boolean selected = ((JCheckBox) e.getSource()).isSelected();
            SettingsExceptionHandlerAction.this.exceptionHandlerSelectionButton.setEnabled(selected);
            SettingsExceptionHandlerAction.this.exceptionHandlerDisplay.setEnabled(selected);
        }
    }

    /// Associated action class: selecting exception handler file. Attached to
    private class ExceptionHandlerSelectionAction implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser chooser = new JFileChooser();
            String pathname = OTHER_SETTINGS.getExceptionHandler();
            final File file = new File(pathname);
            if (file.exists())
                chooser.setSelectedFile(file);
            final int result = chooser.showOpenDialog(Globals.gui);
            if (result == JFileChooser.APPROVE_OPTION) {
                pathname = chooser.getSelectedFile().getPath();// .replaceAll("\\\\","/");
                SettingsExceptionHandlerAction.this.exceptionHandlerDisplay.setText(pathname);
            }
        }
    }

}
