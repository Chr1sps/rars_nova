package rars.venus.settings;

import rars.Globals;
import rars.riscv.hardware.MemoryConfiguration;
import rars.riscv.hardware.MemoryConfigurations;
import rars.simulator.Simulator;
import rars.util.Binary;
import rars.venus.FileStatus;
import rars.venus.GuiAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Settings menu item for text editor settings.
 */
public class SettingsMemoryConfigurationAction extends GuiAction {
    /**
     * Create a new SettingsEditorAction. Has all the GuiAction parameters.
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     */
    public SettingsMemoryConfigurationAction(final String name, final Icon icon, final String descrip,
                                             final Integer mnemonic, final KeyStroke accel) {
        super(name, icon, descrip, mnemonic, accel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final JDialog configDialog = new MemoryConfigurationDialog(Globals.getGui(), "Memory Configuration", true);
        configDialog.setVisible(true);
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // Private class to do all the work!
    //
    private static class MemoryConfigurationDialog extends JDialog implements ActionListener {
        JTextField[] addressDisplay;
        JLabel[] nameDisplay;
        ConfigurationButton selectedConfigurationButton, initialConfigurationButton;

        private MemoryConfigurationDialog(final Frame owner, final String title, final boolean modality) {
            super(owner, title, modality);
            this.setContentPane(this.buildDialogPanel());
            this.setDefaultCloseOperation(
                    JDialog.DO_NOTHING_ON_CLOSE);
            this.addWindowListener(
                    new WindowAdapter() {
                        @Override
                        public void windowClosing(final WindowEvent we) {
                            MemoryConfigurationDialog.this.performClose();
                        }
                    });
            this.pack();
            this.setLocationRelativeTo(owner);
        }

        private JPanel buildDialogPanel() {
            final JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            final JPanel configInfo = new JPanel(new FlowLayout());
            MemoryConfigurations.buildConfigurationCollection();
            configInfo.add(this.buildConfigChooser());
            configInfo.add(this.buildConfigDisplay());
            dialogPanel.add(configInfo);
            dialogPanel.add(this.buildControlPanel(), BorderLayout.SOUTH);
            return dialogPanel;
        }

        private Component buildConfigChooser() {
            final JPanel chooserPanel = new JPanel(new GridLayout(4, 1));
            final ButtonGroup choices = new ButtonGroup();
            final Iterator<MemoryConfiguration> configurationsIterator = MemoryConfigurations.getConfigurationsIterator();
            while (configurationsIterator.hasNext()) {
                final MemoryConfiguration config = configurationsIterator.next();
                final ConfigurationButton button = new ConfigurationButton(config);
                button.addActionListener(this);
                if (button.isSelected()) {
                    this.selectedConfigurationButton = button;
                    this.initialConfigurationButton = button;
                }
                choices.add(button);
                chooserPanel.add(button);
            }
            chooserPanel.setBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Configuration"));
            return chooserPanel;
        }

        private Component buildConfigDisplay() {
            final JPanel displayPanel = new JPanel();
            final MemoryConfiguration config = MemoryConfigurations.getCurrentConfiguration();
            final String[] configurationItemNames = config.getConfigurationItemNames();
            final int numItems = configurationItemNames.length;
            final JPanel namesPanel = new JPanel(new GridLayout(numItems, 1));
            final JPanel valuesPanel = new JPanel(new GridLayout(numItems, 1));
            final Font monospaced = new Font("Monospaced", Font.PLAIN, 12);
            this.nameDisplay = new JLabel[numItems];
            // for (int i=numItems-1; i >= 0; i--) {
            // namesPanel.add(new JLabel(configurationItemNames[i]));
            // }
            this.addressDisplay = new JTextField[numItems];
            for (int i = 0; i < numItems; i++) {
                this.nameDisplay[i] = new JLabel();
                this.addressDisplay[i] = new JTextField();
                this.addressDisplay[i].setEditable(false);
                this.addressDisplay[i].setFont(monospaced);
            }
            // Display vertically from high to low memory addresses so
            // add the components in reverse order.
            for (int i = this.addressDisplay.length - 1; i >= 0; i--) {
                namesPanel.add(this.nameDisplay[i]);
                valuesPanel.add(this.addressDisplay[i]);
            }
            this.setConfigDisplay(config);
            final Box columns = Box.createHorizontalBox();
            columns.add(valuesPanel);
            columns.add(Box.createHorizontalStrut(6));
            columns.add(namesPanel);
            displayPanel.add(columns);
            return displayPanel;
        }

        // Carry out action for the radio buttons.
        @Override
        public void actionPerformed(final ActionEvent e) {
            final MemoryConfiguration config = ((ConfigurationButton) e.getSource()).getConfiguration();
            this.setConfigDisplay(config);
            this.selectedConfigurationButton = (ConfigurationButton) e.getSource();
        }

        // Row of control buttons to be placed along the button of the dialog
        private Component buildControlPanel() {
            final Box controlPanel = Box.createHorizontalBox();
            final JButton okButton = new JButton("Apply and Close");
            okButton.setToolTipText(SettingsHighlightingAction.CLOSE_TOOL_TIP_TEXT);
            okButton.addActionListener(
                    e -> {
                        this.performApply();
                        this.performClose();
                    });
            final JButton applyButton = new JButton("Apply");
            applyButton.setToolTipText(SettingsHighlightingAction.APPLY_TOOL_TIP_TEXT);
            applyButton.addActionListener(
                    e -> this.performApply());
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText(SettingsHighlightingAction.CANCEL_TOOL_TIP_TEXT);
            cancelButton.addActionListener(
                    e -> this.performClose());
            final JButton resetButton = new JButton("Reset");
            resetButton.setToolTipText(SettingsHighlightingAction.RESET_TOOL_TIP_TEXT);
            resetButton.addActionListener(
                    e -> this.performReset());
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(applyButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        private void performApply() {
            if (MemoryConfigurations.setCurrentConfiguration(this.selectedConfigurationButton.getConfiguration())) {
                Globals.getSettings().setMemoryConfiguration(
                        this.selectedConfigurationButton.getConfiguration().getConfigurationIdentifier());
                Globals.getGui().getRegistersPane().getRegistersWindow().clearHighlighting();
                Globals.getGui().getRegistersPane().getRegistersWindow().updateRegisters();
                Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateBaseAddressComboBox();
                // 21 July 2009 Re-assemble if the situation demands it to maintain consistency.
                if (FileStatus.get() == FileStatus.RUNNABLE ||
                        FileStatus.get() == FileStatus.RUNNING ||
                        FileStatus.get() == FileStatus.TERMINATED) {
                    // Stop execution if executing -- should NEVER happen because this
                    // Action's widget is disabled during MIPS execution.
                    if (FileStatus.get() == FileStatus.RUNNING) {
                        Simulator.getInstance().stopExecution();
                    }
                    Globals.getGui().getRunAssembleAction().actionPerformed(null);
                }
            }
        }

        private void performClose() {
            this.setVisible(false);
            this.dispose();
        }

        private void performReset() {
            this.selectedConfigurationButton = this.initialConfigurationButton;
            this.selectedConfigurationButton.setSelected(true);
            this.setConfigDisplay(this.selectedConfigurationButton.getConfiguration());
        }

        // Set name values in JLabels and address values in the JTextFields
        private void setConfigDisplay(final MemoryConfiguration config) {
            final String[] configurationItemNames = config.getConfigurationItemNames();
            final int[] configurationItemValues = config.getConfigurationItemValues();
            // Will use TreeMap to extract list of address-name pairs sorted by
            // hex-stringified address. This will correctly handle kernel addresses,
            // whose int values are negative and thus normal sorting yields incorrect
            // results. There can be duplicate addresses, so I concatenate the name
            // onto the address to make each first unique. Then slice off the name upon
            // extraction.
            final TreeMap<String, String> treeSortedByAddress = new TreeMap<>();
            for (int i = 0; i < configurationItemValues.length; i++) {
                treeSortedByAddress.put(Binary.intToHexString(configurationItemValues[i]) + configurationItemNames[i],
                        configurationItemNames[i]);
            }
            final Iterator<Map.Entry<String, String>> setSortedByAddress = treeSortedByAddress.entrySet().iterator();
            Map.Entry<String, String> pair;
            final int addressStringLength = Binary.intToHexString(configurationItemValues[0]).length();
            for (int i = 0; i < configurationItemValues.length; i++) {
                pair = setSortedByAddress.next();
                this.nameDisplay[i].setText(pair.getValue());
                this.addressDisplay[i].setText(pair.getKey().substring(0, addressStringLength));
            }
        }

    }

    // Handy class to connect button to its configuration...
    private static class ConfigurationButton extends JRadioButton {
        private final MemoryConfiguration configuration;

        public ConfigurationButton(final MemoryConfiguration config) {
            super(config.getConfigurationName(), config == MemoryConfigurations.getCurrentConfiguration());
            this.configuration = config;
        }

        public MemoryConfiguration getConfiguration() {
            return this.configuration;
        }

    }

}
