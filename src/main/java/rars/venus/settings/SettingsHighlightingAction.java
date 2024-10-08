package rars.venus.settings;

import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.Settings;
import rars.venus.ExecutePane;
import rars.venus.GuiAction;
import rars.venus.MonoRightCellRenderer;
import rars.venus.util.AbstractFontSettingDialog;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

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
public class SettingsHighlightingAction extends GuiAction {

    /**
     * Constant <code>CLOSE_TOOL_TIP_TEXT="Apply current settings and close dialog"</code>
     */
    public static final String CLOSE_TOOL_TIP_TEXT = "Apply current settings and close dialog";
    /**
     * Constant <code>APPLY_TOOL_TIP_TEXT="Apply current settings now and leave di"{trunked}</code>
     */
    public static final String APPLY_TOOL_TIP_TEXT = "Apply current settings now and leave dialog open";
    /**
     * Constant <code>RESET_TOOL_TIP_TEXT="Reset to initial settings without apply"{trunked}</code>
     */
    public static final String RESET_TOOL_TIP_TEXT = "Reset to initial settings without applying";
    /**
     * Constant <code>CANCEL_TOOL_TIP_TEXT="Close dialog without applying current s"{trunked}</code>
     */
    public static final String CANCEL_TOOL_TIP_TEXT = "Close dialog without applying current settings";
    // NOTE: These must follow same sequence and buttons must
    // follow this sequence too!
    private static final int[] backgroundSettingPositions = {
            Settings.TEXTSEGMENT_HIGHLIGHT_BACKGROUND,
            Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND,
            Settings.DATASEGMENT_HIGHLIGHT_BACKGROUND,
            Settings.REGISTER_HIGHLIGHT_BACKGROUND,
            Settings.EVEN_ROW_BACKGROUND,
            Settings.ODD_ROW_BACKGROUND
    };
    private static final int[] foregroundSettingPositions = {
            Settings.TEXTSEGMENT_HIGHLIGHT_FOREGROUND,
            Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND,
            Settings.DATASEGMENT_HIGHLIGHT_FOREGROUND,
            Settings.REGISTER_HIGHLIGHT_FOREGROUND,
            Settings.EVEN_ROW_FOREGROUND,
            Settings.ODD_ROW_FOREGROUND
    };
    private static final int[] fontSettingPositions = {
            Settings.TEXTSEGMENT_HIGHLIGHT_FONT,
            Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT,
            Settings.DATASEGMENT_HIGHLIGHT_FONT,
            Settings.REGISTER_HIGHLIGHT_FONT,
            Settings.EVEN_ROW_FONT,
            Settings.ODD_ROW_FONT
    };
    private static final int gridVGap = 2;
    private static final int gridHGap = 2;
    // Tool tips for color buttons
    private static final String SAMPLE_TOOL_TIP_TEXT = "Preview based on background and text color settings";
    private static final String BACKGROUND_TOOL_TIP_TEXT = "Click, to select background color";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
    private static final String FONT_TOOL_TIP_TEXT = "Click, to select text font";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select default color (disables color select buttons)";
    // Tool tips for the data and register highlighting enable/disable controls
    private static final String DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Data Segment window";
    private static final String REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Register windows";
    private static final String fontButtonText = "font";
    JDialog highlightDialog;
    JButton[] backgroundButtons;
    JButton[] foregroundButtons;
    JButton[] fontButtons;
    JCheckBox[] defaultCheckBoxes;
    // Tool tips for the control buttons along the bottom
    JLabel[] samples;
    Color[] currentNondefaultBackground, currentNondefaultForeground;
    Color[] initialSettingsBackground, initialSettingsForeground;
    Font[] initialFont, currentFont, currentNondefaultFont;
    JButton dataHighlightButton, registerHighlightButton;
    boolean currentDataHighlightSetting, initialDataHighlightSetting;
    boolean currentRegisterHighlightSetting, initialRegisterHighlightSetting;

    /**
     * Create a new SettingsEditorAction. Has all the GuiAction parameters.
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     */
    public SettingsHighlightingAction(final String name, final Icon icon, final String descrip,
                                      final Integer mnemonic, final KeyStroke accel) {
        super(name, icon, descrip, mnemonic, accel);
    }

    private static String getHighlightControlText(final boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    /**
     * {@inheritDoc}
     * <p>
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.highlightDialog = new JDialog(Globals.getGui(), "Runtime Table Highlighting Colors and Fonts", true);
        this.highlightDialog.setContentPane(this.buildDialogPanel());
        this.highlightDialog.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        this.highlightDialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent we) {
                        SettingsHighlightingAction.this.closeDialog();
                    }
                });
        this.highlightDialog.pack();
        this.highlightDialog.setLocationRelativeTo(Globals.getGui());
        this.highlightDialog.setVisible(true);
    }

    // The dialog box that appears when menu item is selected.
    private JPanel buildDialogPanel() {
        final JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        final JPanel patches = new JPanel(new GridLayout(SettingsHighlightingAction.backgroundSettingPositions.length, 4, SettingsHighlightingAction.gridVGap, SettingsHighlightingAction.gridHGap));
        this.currentNondefaultBackground = new Color[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.currentNondefaultForeground = new Color[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.initialSettingsBackground = new Color[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.initialSettingsForeground = new Color[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.initialFont = new Font[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.currentFont = new Font[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.currentNondefaultFont = new Font[SettingsHighlightingAction.backgroundSettingPositions.length];

        this.backgroundButtons = new JButton[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.foregroundButtons = new JButton[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.fontButtons = new JButton[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.defaultCheckBoxes = new JCheckBox[SettingsHighlightingAction.backgroundSettingPositions.length];
        this.samples = new JLabel[SettingsHighlightingAction.backgroundSettingPositions.length];
        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            this.backgroundButtons[i] = new ColorSelectButton();
            this.foregroundButtons[i] = new ColorSelectButton();
            this.fontButtons[i] = new JButton(SettingsHighlightingAction.fontButtonText);
            this.defaultCheckBoxes[i] = new JCheckBox();
            this.samples[i] = new JLabel(" preview ");
            this.backgroundButtons[i].addActionListener(new BackgroundChanger(i));
            this.foregroundButtons[i].addActionListener(new ForegroundChanger(i));
            this.fontButtons[i].addActionListener(new FontChanger(i));
            this.defaultCheckBoxes[i].addItemListener(new DefaultChanger(i));
            this.samples[i].setToolTipText(SettingsHighlightingAction.SAMPLE_TOOL_TIP_TEXT);
            this.backgroundButtons[i].setToolTipText(SettingsHighlightingAction.BACKGROUND_TOOL_TIP_TEXT);
            this.foregroundButtons[i].setToolTipText(SettingsHighlightingAction.FOREGROUND_TOOL_TIP_TEXT);
            this.fontButtons[i].setToolTipText(SettingsHighlightingAction.FONT_TOOL_TIP_TEXT);
            this.defaultCheckBoxes[i].setToolTipText(SettingsHighlightingAction.DEFAULT_TOOL_TIP_TEXT);
        }

        this.initializeButtonColors();

        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            patches.add(this.backgroundButtons[i]);
            patches.add(this.foregroundButtons[i]);
            patches.add(this.fontButtons[i]);
            patches.add(this.defaultCheckBoxes[i]);
        }

        final JPanel descriptions = new JPanel(new GridLayout(SettingsHighlightingAction.backgroundSettingPositions.length, 1, SettingsHighlightingAction.gridVGap, SettingsHighlightingAction.gridHGap));
        // Note the labels have to match buttons by position...
        descriptions.add(new JLabel("Text Segment highlighting", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Text Segment Delay Slot highlighting", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Data Segment highlighting *", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Register highlighting *", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Even row normal", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Odd row normal", SwingConstants.RIGHT));

        final JPanel sample = new JPanel(new GridLayout(SettingsHighlightingAction.backgroundSettingPositions.length, 1, SettingsHighlightingAction.gridVGap, SettingsHighlightingAction.gridHGap));
        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            sample.add(this.samples[i]);
        }

        final JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // create deaf, dumb and blind checkbox, for illustration
        final JCheckBox illustrate = new JCheckBox() {
            @Override
            protected void processMouseEvent(final MouseEvent e) {
            }

            @Override
            protected void processKeyEvent(final KeyEvent e) {
            }
        };
        illustrate.setSelected(true);
        instructions.add(illustrate);
        instructions.add(new JLabel("= use default colors (disables color selection buttons)"));
        final int spacer = 10;
        final Box mainArea = Box.createHorizontalBox();
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(descriptions);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(sample);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(patches);

        contents.add(mainArea, BorderLayout.EAST);
        contents.add(instructions, BorderLayout.NORTH);

        // Control highlighting enable/disable for Data Segment window and Register
        // windows
        final JPanel dataRegisterHighlightControl = new JPanel(new GridLayout(2, 1));
        this.dataHighlightButton = new JButton();
        this.dataHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(this.currentDataHighlightSetting));
        this.dataHighlightButton.setToolTipText(SettingsHighlightingAction.DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        this.dataHighlightButton.addActionListener(
                e -> {
                    SettingsHighlightingAction.this.currentDataHighlightSetting = !SettingsHighlightingAction.this.currentDataHighlightSetting;
                    SettingsHighlightingAction.this.dataHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(SettingsHighlightingAction.this.currentDataHighlightSetting));
                });
        this.registerHighlightButton = new JButton();
        this.registerHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(this.currentRegisterHighlightSetting));
        this.registerHighlightButton.setToolTipText(SettingsHighlightingAction.REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        this.registerHighlightButton.addActionListener(
                e -> {
                    SettingsHighlightingAction.this.currentRegisterHighlightSetting = !SettingsHighlightingAction.this.currentRegisterHighlightSetting;
                    SettingsHighlightingAction.this.registerHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(SettingsHighlightingAction.this.currentRegisterHighlightSetting));
                });
        final JPanel dataHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JPanel registerHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataHighlightPanel.add(new JLabel("* Data Segment highlighting is"));
        dataHighlightPanel.add(this.dataHighlightButton);
        registerHighlightPanel.add(new JLabel("* Register highlighting is"));
        registerHighlightPanel.add(this.registerHighlightButton);
        dataRegisterHighlightControl.setBorder(new LineBorder(Color.BLACK));
        dataRegisterHighlightControl.add(dataHighlightPanel);
        dataRegisterHighlightControl.add(registerHighlightPanel);

        // Bottom row - the control buttons for Apply&Close, Apply, Cancel
        final Box controlPanel = Box.createHorizontalBox();
        final JButton okButton = new JButton("Apply and Close");
        okButton.setToolTipText(SettingsHighlightingAction.CLOSE_TOOL_TIP_TEXT);
        okButton.addActionListener(
                e -> {
                    SettingsHighlightingAction.this.setHighlightingSettings();
                    SettingsHighlightingAction.this.closeDialog();
                });
        final JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(SettingsHighlightingAction.APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener(
                e -> SettingsHighlightingAction.this.setHighlightingSettings());
        final JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText(SettingsHighlightingAction.RESET_TOOL_TIP_TEXT);
        resetButton.addActionListener(
                e -> SettingsHighlightingAction.this.resetButtonColors());
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(SettingsHighlightingAction.CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener(
                e -> SettingsHighlightingAction.this.closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(applyButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalGlue());

        final JPanel allControls = new JPanel(new GridLayout(2, 1));
        allControls.add(dataRegisterHighlightControl);
        allControls.add(controlPanel);
        contents.add(allControls, BorderLayout.SOUTH);
        return contents;
    }

    // Called once, upon dialog setup.
    private void initializeButtonColors() {
        final Settings settings = Globals.getSettings();
        final LineBorder lineBorder = new LineBorder(Color.BLACK);
        Color backgroundSetting, foregroundSetting;
        Font fontSetting;
        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            backgroundSetting = settings.getColorSettingByPosition(SettingsHighlightingAction.backgroundSettingPositions[i]);
            foregroundSetting = settings.getColorSettingByPosition(SettingsHighlightingAction.foregroundSettingPositions[i]);
            fontSetting = settings.getFontByPosition(SettingsHighlightingAction.fontSettingPositions[i]);
            this.backgroundButtons[i].setBackground(backgroundSetting);
            this.foregroundButtons[i].setBackground(foregroundSetting);
            this.fontButtons[i].setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT); // fontSetting);
            this.fontButtons[i].setMargin(new Insets(4, 4, 4, 4));
            this.initialFont[i] = this.currentFont[i] = fontSetting;
            this.currentNondefaultBackground[i] = backgroundSetting;
            this.currentNondefaultForeground[i] = foregroundSetting;
            this.currentNondefaultFont[i] = fontSetting;
            this.initialSettingsBackground[i] = backgroundSetting;
            this.initialSettingsForeground[i] = foregroundSetting;
            this.samples[i].setOpaque(true); // otherwise, background color will not be rendered
            this.samples[i].setBorder(lineBorder);
            this.samples[i].setBackground(backgroundSetting);
            this.samples[i].setForeground(foregroundSetting);
            this.samples[i].setFont(fontSetting);
            final boolean usingDefaults = backgroundSetting
                    .equals(settings.getDefaultColorSettingByPosition(SettingsHighlightingAction.backgroundSettingPositions[i])) &&
                    foregroundSetting.equals(settings.getDefaultColorSettingByPosition(SettingsHighlightingAction.foregroundSettingPositions[i]))
                    &&
                    Objects.equals(fontSetting, Settings.getDefaultFontByPosition(SettingsHighlightingAction.fontSettingPositions[i]));
            this.defaultCheckBoxes[i].setSelected(usingDefaults);
            this.backgroundButtons[i].setEnabled(!usingDefaults);
            this.foregroundButtons[i].setEnabled(!usingDefaults);
            this.fontButtons[i].setEnabled(!usingDefaults);
        }
        this.currentDataHighlightSetting = this.initialDataHighlightSetting = settings
                .getBooleanSetting(Settings.Bool.DATA_SEGMENT_HIGHLIGHTING);
        this.currentRegisterHighlightSetting = this.initialRegisterHighlightSetting = settings
                .getBooleanSetting(Settings.Bool.REGISTERS_HIGHLIGHTING);
    }

    // Set the color settings according to current button colors. Occurs when
    // "Apply" selected.
    private void setHighlightingSettings() {
        final Settings settings = Globals.getSettings();
        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            settings.setColorSettingByPosition(SettingsHighlightingAction.backgroundSettingPositions[i], this.backgroundButtons[i].getBackground());
            settings.setColorSettingByPosition(SettingsHighlightingAction.foregroundSettingPositions[i], this.foregroundButtons[i].getBackground());
            settings.setFontByPosition(SettingsHighlightingAction.fontSettingPositions[i], this.samples[i].getFont());// fontButtons[i].getFont());
        }
        settings.setBooleanSetting(Settings.Bool.DATA_SEGMENT_HIGHLIGHTING, this.currentDataHighlightSetting);
        settings.setBooleanSetting(Settings.Bool.REGISTERS_HIGHLIGHTING, this.currentRegisterHighlightSetting);
        final ExecutePane executePane = Globals.getGui().getMainPane().getExecutePane();
        executePane.getRegistersWindow().refresh();
        executePane.getControlAndStatusWindow().refresh();
        executePane.getFloatingPointWindow().refresh();
        // If a successful assembly has occurred, the various panes will be populated
        // with tables
        // and we want to apply the new settings. If it has NOT occurred, there are no
        // tables
        // in the Data and Text segment windows so we don't want to disturb them.
        // In the latter case, the component count for the Text segment window is 0 (but
        // is 1
        // for Data segment window).
        if (executePane.getTextSegmentWindow().getContentPane().getComponentCount() > 0) {
            executePane.getDataSegmentWindow().updateValues();
            executePane.getTextSegmentWindow().highlightStepAtPC();
        }
    }

    // Called when Reset selected.
    private void resetButtonColors() {
        final Settings settings = Globals.getSettings();
        this.dataHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(this.initialDataHighlightSetting));
        this.registerHighlightButton.setText(SettingsHighlightingAction.getHighlightControlText(this.initialRegisterHighlightSetting));
        Color backgroundSetting, foregroundSetting;
        Font fontSetting;
        for (int i = 0; i < SettingsHighlightingAction.backgroundSettingPositions.length; i++) {
            backgroundSetting = this.initialSettingsBackground[i];
            foregroundSetting = this.initialSettingsForeground[i];
            fontSetting = this.initialFont[i];
            this.backgroundButtons[i].setBackground(backgroundSetting);
            this.foregroundButtons[i].setBackground(foregroundSetting);
            // fontButtons[i].setFont(fontSetting);
            this.samples[i].setBackground(backgroundSetting);
            this.samples[i].setForeground(foregroundSetting);
            this.samples[i].setFont(fontSetting);
            final boolean usingDefaults = backgroundSetting
                    .equals(settings.getDefaultColorSettingByPosition(SettingsHighlightingAction.backgroundSettingPositions[i])) &&
                    foregroundSetting.equals(settings.getDefaultColorSettingByPosition(SettingsHighlightingAction.foregroundSettingPositions[i]))
                    &&
                    fontSetting.equals(Settings.getDefaultFontByPosition(SettingsHighlightingAction.fontSettingPositions[i]));
            this.defaultCheckBoxes[i].setSelected(usingDefaults);
            this.backgroundButtons[i].setEnabled(!usingDefaults);
            this.foregroundButtons[i].setEnabled(!usingDefaults);
            this.fontButtons[i].setEnabled(!usingDefaults);
        }
    }

    // We're finished with this modal dialog.
    private void closeDialog() {
        this.highlightDialog.setVisible(false);
        this.highlightDialog.dispose();
    }

    ///////////////////////////////////////////////////////////////////
    //
    // Modal dialog to set a font.
    //
    private static class FontSettingDialog extends AbstractFontSettingDialog {
        private boolean resultOK;

        public FontSettingDialog(final Frame owner, final String title, final Font currentFont) {
            super(owner, title, true, currentFont);
        }

        private @Nullable Font showDialog() {
            this.resultOK = true;
            // Because dialog is modal, this blocks until user terminates the dialog.
            this.setVisible(true);
            return this.resultOK ? this.getFont() : null;
        }

        @Override
        protected void closeDialog() {
            this.setVisible(false);
        }

        private void performOK() {
            this.resultOK = true;
        }

        private void performCancel() {
            this.resultOK = false;
        }

        // Control buttons for the dialog.
        @Override
        protected Component buildControlPanel() {
            final Box controlPanel = Box.createHorizontalBox();
            final JButton okButton = new JButton("OK");
            okButton.addActionListener(
                    e -> {
                        FontSettingDialog.this.performOK();
                        FontSettingDialog.this.closeDialog();
                    });
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(
                    e -> {
                        FontSettingDialog.this.performCancel();
                        FontSettingDialog.this.closeDialog();
                    });
            final JButton resetButton = new JButton("Reset");
            resetButton.addActionListener(
                    e -> FontSettingDialog.this.reset());
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        // required by Abstract super class but not used here.

        /**
         * {@inheritDoc}
         */
        @Override
        protected void apply(final Font font) {

        }

    }

    /////////////////////////////////////////////////////////////////
    //
    // Class that handles click on the background selection button
    //
    private class BackgroundChanger implements ActionListener {
        private final int position;

        public BackgroundChanger(final int pos) {
            this.position = pos;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton) e.getSource();
            final Color newColor = JColorChooser.showDialog(null, "Set Background Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                SettingsHighlightingAction.this.currentNondefaultBackground[this.position] = newColor;
                SettingsHighlightingAction.this.samples[this.position].setBackground(newColor);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    // Class that handles click on the foreground selection button
    //
    private class ForegroundChanger implements ActionListener {
        private final int position;

        public ForegroundChanger(final int pos) {
            this.position = pos;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final JButton button = (JButton) e.getSource();
            final Color newColor = JColorChooser.showDialog(null, "Set Text Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                SettingsHighlightingAction.this.currentNondefaultForeground[this.position] = newColor;
                SettingsHighlightingAction.this.samples[this.position].setForeground(newColor);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    // Class that handles click on the font select button
    //
    private class FontChanger implements ActionListener {
        private final int position;

        public FontChanger(final int pos) {
            this.position = pos;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
//            e.getSource();
            final FontSettingDialog fontDialog = new FontSettingDialog(null, "Select Text Font", SettingsHighlightingAction.this.samples[this.position].getFont());
            final Font newFont = fontDialog.showDialog();
            if (newFont != null) {
                // button.setFont(newFont);
                SettingsHighlightingAction.this.samples[this.position].setFont(newFont);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    // Class that handles action (check, uncheck) on the Default checkbox.
    //
    private class DefaultChanger implements ItemListener {
        private final int position;

        public DefaultChanger(final int pos) {
            this.position = pos;
        }

        @Override
        public void itemStateChanged(final ItemEvent e) {
            // If selected: disable buttons, set their bg values from default setting, set
            // sample bg & fg
            // If deselected: enable buttons, set their bg values from current setting, set
            // sample bg & bg
            final Color newBackground;
            final Color newForeground;
            final Font newFont;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                SettingsHighlightingAction.this.backgroundButtons[this.position].setEnabled(false);
                SettingsHighlightingAction.this.foregroundButtons[this.position].setEnabled(false);
                SettingsHighlightingAction.this.fontButtons[this.position].setEnabled(false);
                newBackground = Globals.getSettings()
                        .getDefaultColorSettingByPosition(SettingsHighlightingAction.backgroundSettingPositions[this.position]);
                newForeground = Globals.getSettings()
                        .getDefaultColorSettingByPosition(SettingsHighlightingAction.foregroundSettingPositions[this.position]);
                newFont = Settings.getDefaultFontByPosition(SettingsHighlightingAction.fontSettingPositions[this.position]);
                SettingsHighlightingAction.this.currentNondefaultBackground[this.position] = SettingsHighlightingAction.this.backgroundButtons[this.position].getBackground();
                SettingsHighlightingAction.this.currentNondefaultForeground[this.position] = SettingsHighlightingAction.this.foregroundButtons[this.position].getBackground();
                SettingsHighlightingAction.this.currentNondefaultFont[this.position] = SettingsHighlightingAction.this.samples[this.position].getFont();
            } else {
                SettingsHighlightingAction.this.backgroundButtons[this.position].setEnabled(true);
                SettingsHighlightingAction.this.foregroundButtons[this.position].setEnabled(true);
                SettingsHighlightingAction.this.fontButtons[this.position].setEnabled(true);
                newBackground = SettingsHighlightingAction.this.currentNondefaultBackground[this.position];
                newForeground = SettingsHighlightingAction.this.currentNondefaultForeground[this.position];
                newFont = SettingsHighlightingAction.this.currentNondefaultFont[this.position];
            }
            SettingsHighlightingAction.this.backgroundButtons[this.position].setBackground(newBackground);
            SettingsHighlightingAction.this.foregroundButtons[this.position].setBackground(newForeground);
            // fontButtons[position].setFont(newFont);
            SettingsHighlightingAction.this.samples[this.position].setBackground(newBackground);
            SettingsHighlightingAction.this.samples[this.position].setForeground(newForeground);
            SettingsHighlightingAction.this.samples[this.position].setFont(newFont);
        }
    }

}

/////////////////////////////////////////////////////////////////
//
// Dinky little custom button class to modify border based on
// whether enabled or not. The default behavior does not work
// well on buttons with black background.
class ColorSelectButton extends JButton {
    private static final Border ColorSelectButtonEnabledBorder = new BevelBorder(BevelBorder.RAISED, Color.WHITE,
            Color.GRAY);
    private static final Border ColorSelectButtonDisabledBorder = new LineBorder(Color.GRAY, 2);

    @Override
    public void setEnabled(final boolean status) {
        super.setEnabled(status);
        this.setBorder(status ? ColorSelectButton.ColorSelectButtonEnabledBorder : ColorSelectButton.ColorSelectButtonDisabledBorder);
    }
}
