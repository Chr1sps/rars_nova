package rars.venus.settings;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.riscv.hardware.MemoryConfiguration;
import rars.util.BinaryUtilsKt;
import rars.venus.FileStatus;
import rars.venus.GuiAction;
import rars.venus.VenusUI;

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

import static rars.Globals.FONT_SETTINGS;
import static rars.Globals.OTHER_SETTINGS;

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
public final class SettingsMemoryConfigurationAction extends GuiAction {
    private static final String[] configurationItemNames = {
        ".text base address",
        "data segment base address",
        ".extern base address",
        "global pointer (gp)",
        ".data base address",
        "heap base address",
        "stack pointer (sp)",
        "stack base address",
        "user space high address",
        "kernel space base address",
        "MMIO base address",
        "kernel space high address",
        "data segment limit address",
        "text limit address",
        "stack limit address",
        "memory map limit address"
    };

    public SettingsMemoryConfigurationAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final @NotNull VenusUI mainUI
    ) {
        super(name, icon, descrip, mnemonic, accel, mainUI);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final JDialog configDialog = new MemoryConfigurationDialog(this.mainUI, "Memory Configuration", true);
        configDialog.setVisible(true);
    }

    // Handy class to connect button to its configuration...
    private static class ConfigurationButton extends JRadioButton {
        public final @NotNull MemoryConfiguration configuration;

        public ConfigurationButton(final @NotNull MemoryConfiguration config) {
            super(config.description, config == OTHER_SETTINGS.getMemoryConfiguration());
            this.configuration = config;
        }
    }

    /// Private class to do all the work!
    private final class MemoryConfigurationDialog extends JDialog implements ActionListener {
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
            configInfo.add(this.buildConfigChooser());
            configInfo.add(this.buildConfigDisplay());
            dialogPanel.add(configInfo);
            dialogPanel.add(this.buildControlPanel(), BorderLayout.SOUTH);
            return dialogPanel;
        }

        private Component buildConfigChooser() {
            final JPanel chooserPanel = new JPanel(new GridLayout(4, 1));
            final ButtonGroup choices = new ButtonGroup();
            for (final var configuration : MemoryConfiguration.values()) {
                final var button = new ConfigurationButton(configuration);
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
            final var config = OTHER_SETTINGS.getMemoryConfiguration();
            final int numItems = configurationItemNames.length;
            final JPanel namesPanel = new JPanel(new GridLayout(numItems, 1));
            final JPanel valuesPanel = new JPanel(new GridLayout(numItems, 1));
            this.nameDisplay = new JLabel[numItems];
            this.addressDisplay = new JTextField[numItems];
            for (int i = 0; i < numItems; i++) {
                this.nameDisplay[i] = new JLabel();
                final var textField = new JTextField();
                textField.setEditable(false);
                textField.setFont(FONT_SETTINGS.getCurrentFont());
                textField.setFocusable(false);
                this.addressDisplay[i] = textField;
            }
            FONT_SETTINGS.onChangeListenerHook.subscribe(ignored -> {
                for (final var textField : this.addressDisplay) {
                    textField.setFont(FONT_SETTINGS.getCurrentFont());
                }
            });
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
            final var config = ((ConfigurationButton) e.getSource()).configuration;
            this.setConfigDisplay(config);
            this.selectedConfigurationButton = (ConfigurationButton) e.getSource();
        }

        // Row of control buttons to be placed along the button of the dialog
        private Component buildControlPanel() {
            final Box controlPanel = Box.createHorizontalBox();
            final JButton okButton = new JButton("Apply and Close");
            okButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
            okButton.addActionListener(
                e -> {
                    this.performApply();
                    this.performClose();
                });
            final JButton applyButton = new JButton("Apply");
            applyButton.setToolTipText(APPLY_TOOL_TIP_TEXT);
            applyButton.addActionListener(
                e -> this.performApply());
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText(CANCEL_TOOL_TIP_TEXT);
            cancelButton.addActionListener(
                e -> this.performClose());
            final JButton resetButton = new JButton("Reset");
            resetButton.setToolTipText(RESET_TOOL_TIP_TEXT);
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
            final var currentConfiguration = OTHER_SETTINGS.getMemoryConfiguration();
            final var newConfiguration = this.selectedConfigurationButton.configuration;
            if (newConfiguration != currentConfiguration) {
                OTHER_SETTINGS.setMemoryConfigurationAndSave(newConfiguration);
                Globals.setupGlobalMemoryConfiguration(newConfiguration);
                SettingsMemoryConfigurationAction.this.mainUI.registersPane.getRegistersWindow().clearHighlighting();
                SettingsMemoryConfigurationAction.this.mainUI.registersPane.getRegistersWindow().updateRegisters();
                SettingsMemoryConfigurationAction.this.mainUI.mainPane.executePane.dataSegment.updateBaseAddressComboBox();
                // 21 July 2009 Re-assemble if the situation demands it to maintain consistency.
                if (FileStatus.getSystemState() == FileStatus.State.RUNNABLE ||
                    FileStatus.getSystemState() == FileStatus.State.RUNNING ||
                    FileStatus.getSystemState() == FileStatus.State.TERMINATED) {
                    // Stop execution if executing -- should NEVER happen because this
                    // Action's widget is disabled during MIPS execution.
                    if (FileStatus.getSystemState() == FileStatus.State.RUNNING) {
                        Globals.SIMULATOR.stopExecution();
                    }
                    SettingsMemoryConfigurationAction.this.mainUI.getRunAssembleAction().actionPerformed(null);
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
            this.setConfigDisplay(this.selectedConfigurationButton.configuration);
        }

        // Set name values in JLabels and address values in the JTextFields
        private void setConfigDisplay(final @NotNull MemoryConfiguration config) {
            final int[] configurationItemValues = {
                config.textBaseAddress,
                config.dataSegmentBaseAddress,
                config.externBaseAddress,
                config.globalPointerAddress,
                config.dataBaseAddress,
                config.heapBaseAddress,
                config.stackPointerAddress,
                config.stackBaseAddress,
                config.userHighAddress,
                config.kernelBaseAddress,
                config.memoryMapBaseAddress,
                config.kernelHighAddress,
                config.dataSegmentLimitAddress,
                config.textLimitAddress,
                config.stackLimitAddress,
                config.memoryMapLimitAddress
            };
            // Will use TreeMap to extract list of address-name pairs sorted by
            // hex-stringified address. This will correctly handle kernel addresses,
            // whose int values are negative and thus normal sorting yields incorrect
            // results. There can be duplicate addresses, so I concatenate the name
            // onto the address to make each key unique. Then slice off the name upon
            // extraction.
            final TreeMap<String, String> treeSortedByAddress = new TreeMap<>();
            for (int i = 0; i < configurationItemValues.length; i++) {
                treeSortedByAddress.put(
                    BinaryUtilsKt.intToHexStringWithPrefix(configurationItemValues[i]) + configurationItemNames[i],
                    configurationItemNames[i]
                );
            }
            final Iterator<Map.Entry<String, String>> setSortedByAddress = treeSortedByAddress.entrySet().iterator();
            final int addressStringLength = BinaryUtilsKt.intToHexStringWithPrefix(configurationItemValues[0])
                .length();
            for (int i = 0; i < configurationItemValues.length; i++) {
                Map.Entry<String, String> pair = setSortedByAddress.next();
                this.nameDisplay[i].setText(pair.getValue());
                this.addressDisplay[i].setText(pair.getKey().substring(0, addressStringLength));
            }
        }

    }

}
