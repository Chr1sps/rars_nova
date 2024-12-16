package rars.venus.settings;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.Settings;
import rars.venus.Editor;
import rars.venus.util.AbstractFontSettingDialog;

import javax.swing.*;
import javax.swing.text.Caret;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

// Concrete font chooser class.
public final class EditorFontDialog extends AbstractFontSettingDialog {
    private static final int gridVGap = 2;
    private static final int gridHGap = 2;
    private static final String SAMPLE_TOOL_TIP_TEXT = "Current setting; modify using buttons to the right";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
    private static final String BOLD_TOOL_TIP_TEXT = "Toggle text bold style";
    private static final String ITALIC_TOOL_TIP_TEXT = "Toggle text italic style";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select defaults (disables buttons)";
    private static final String BOLD_BUTTON_TOOL_TIP_TEXT = "B";
    private static final String ITALIC_BUTTON_TOOL_TIP_TEXT = "I";
    private static final String TAB_SIZE_TOOL_TIP_TEXT = "Current tab size in characters";
    private static final String BLINK_SPINNER_TOOL_TIP_TEXT = "Current blinking rate in milliseconds";
    private static final String BLINK_SAMPLE_TOOL_TIP_TEXT = "Displays current blinking rate";
    private static final String CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT = "Check, to highlight line currently being " +
            "edited";
    private static final String AUTO_INDENT_TOOL_TIP_TEXT = "Check, to enable auto-indent to previous line when Enter" +
            " first is pressed";
    private JButton[] foregroundButtons;
    private JLabel[] samples;
    private JToggleButton[] bold, italic;
    private JCheckBox[] useDefault;
    //    private TokenType[] syntaxStyleIndex;
//    private Map<TokenType, SyntaxStyle> defaultStyles, initialStyles, currentStyles;
    private Font previewFont;
    private JSlider tabSizeSelector;
    private JSpinner tabSizeSpinSelector, blinkRateSpinSelector;
    private JCheckBox lineHighlightCheck, autoIndentCheck;
    private ColorChangerPanel bgChanger, fgChanger, lhChanger, textSelChanger, caretChanger;
    private Caret blinkCaret;
    private JTextField blinkSample;
    private JRadioButton[] popupGuidanceOptions;
    // Flag to indicate whether any syntax style buttons have been clicked
    // since dialog created or most recent "apply".
    private boolean syntaxStylesAction = false;
    private int initialEditorTabSize, initialCaretBlinkRate, initialPopupGuidance;
    private boolean initialLineHighlighting, initialAutoIndent;

    public EditorFontDialog(final Frame owner, final String title, final boolean modality, final Font font) {
        super(owner, title, modality, font);
    }

    // build the dialog here
    @Override
    protected @NotNull JPanel buildDialogPanel() {
        final JPanel dialog = new JPanel(new BorderLayout());
        final JPanel fontDialogPanel = super.buildDialogPanel();
        final JPanel editorStylePanel = this.buildEditorStylePanel();
        final JPanel syntaxStylePanel = this.buildSyntaxStylePanel();
        final JPanel otherSettingsPanel = this.buildOtherSettingsPanel();
        fontDialogPanel.setBorder(BorderFactory.createTitledBorder("Editor Font"));
        editorStylePanel.setBorder(BorderFactory.createTitledBorder("Editor Style"));
        syntaxStylePanel.setBorder(BorderFactory.createTitledBorder("Syntax Styling"));
        otherSettingsPanel.setBorder(BorderFactory.createTitledBorder("Other Editor Settings"));
        dialog.add(fontDialogPanel, BorderLayout.WEST);
        dialog.add(editorStylePanel, BorderLayout.CENTER);
        dialog.add(syntaxStylePanel, BorderLayout.EAST);
        dialog.add(otherSettingsPanel, BorderLayout.SOUTH);
        ///// 4 Aug 2010
        return dialog;
    }

    // Row of control buttons to be placed along the button of the dialog
    @Override
    protected @NotNull Component buildControlPanel() {
        final Box controlPanel = Box.createHorizontalBox();
        final JButton okButton = new JButton("Apply and Close");
        okButton.setToolTipText(SettingsHighlightingAction.CLOSE_TOOL_TIP_TEXT);
        okButton.addActionListener(
                e -> {
                    this.performApply();
                    this.closeDialog();
                });
        final JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(SettingsHighlightingAction.APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener(
                e -> this.performApply());
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(SettingsHighlightingAction.CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener(
                e -> this.closeDialog());
        final JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText(SettingsHighlightingAction.RESET_TOOL_TIP_TEXT);
        resetButton.addActionListener(
                e -> this.reset());
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

    // User has clicked "Apply" or "Apply and Close" button. Required method, is
    // abstract in superclass.
    @Override
    protected void apply(final Font font) {
//            Globals.getSettings().setBooleanSetting(Settings.Bool.GENERIC_TEXT_EDITOR, genericEditorCheck
//            .isSelected());
        Globals.getSettings().setBooleanSetting(Settings.Bool.EDITOR_CURRENT_LINE_HIGHLIGHTING,
                this.lineHighlightCheck.isSelected());
        Globals.getSettings().setBooleanSetting(Settings.Bool.AUTO_INDENT, this.autoIndentCheck.isSelected());
        Globals.getSettings().setCaretBlinkRate((Integer) this.blinkRateSpinSelector.getValue());
        Globals.getSettings().setEditorTabSize(this.tabSizeSelector.getValue());
        this.bgChanger.save();
        this.fgChanger.save();
        this.textSelChanger.save();
        this.caretChanger.save();
        this.lhChanger.save();

        if (this.syntaxStylesAction) {
            for (int i = 0; i < this.syntaxStyleIndex.length; i++) {
                Globals.getSettings().setEditorSyntaxStyleByTokenType(this.syntaxStyleIndex[i],
                        new SyntaxStyle(this.samples[i].getForeground(),
                                this.italic[i].isSelected(), this.bold[i].isSelected()));
            }
            this.syntaxStylesAction = false; // reset
        }
        Globals.getSettings().setEditorFont(font);
        for (int i = 0; i < this.popupGuidanceOptions.length; i++) {
            if (this.popupGuidanceOptions[i].isSelected()) {
                if (i == 0) {
                    Globals.getSettings().setBooleanSetting(Settings.Bool.POPUP_INSTRUCTION_GUIDANCE, false);
                } else {
                    Globals.getSettings().setBooleanSetting(Settings.Bool.POPUP_INSTRUCTION_GUIDANCE, true);
                    Globals.getSettings().setEditorPopupPrefixLength(i);
                }
                break;
            }
        }
    }

    // User has clicked "Reset" button. Put everything back to initial state.
    @Override
    protected void reset() {
        super.reset();
        this.initializeSyntaxStyleChangeables();
        this.resetOtherSettings();
        this.syntaxStylesAction = true;
    }

    // Perform reset on miscellaneous editor settings
    private void resetOtherSettings() {
        this.tabSizeSelector.setValue(this.initialEditorTabSize);
        this.tabSizeSpinSelector.setValue(this.initialEditorTabSize);
        this.lineHighlightCheck.setSelected(this.initialLineHighlighting);
        this.autoIndentCheck.setSelected(this.initialAutoIndent);
        this.blinkRateSpinSelector.setValue(this.initialCaretBlinkRate);
        this.blinkCaret.setBlinkRate(this.initialCaretBlinkRate);
        this.bgChanger.reset();
        this.fgChanger.reset();
        this.lhChanger.reset();
        this.textSelChanger.reset();
        this.caretChanger.reset();
        this.popupGuidanceOptions[this.initialPopupGuidance].setSelected(true);
    }

    // Miscellaneous editor settings (cursor blinking, line highlighting, tab size,
    // etc)
    private @NotNull JPanel buildOtherSettingsPanel() {
        final JPanel otherSettingsPanel = new JPanel();

        // Tab size selector
        this.initialEditorTabSize = Globals.getSettings().getEditorTabSize();
        this.tabSizeSelector = new JSlider(Editor.MIN_TAB_SIZE, Editor.MAX_TAB_SIZE, this.initialEditorTabSize);
        this.tabSizeSelector.setToolTipText(
                "Use slider to select tab size from " + Editor.MIN_TAB_SIZE + " to " + Editor.MAX_TAB_SIZE + ".");
        this.tabSizeSelector.addChangeListener(
                e -> {
                    final Integer value = ((JSlider) e.getSource()).getValue();
                    this.tabSizeSpinSelector.setValue(value);
                });
        final SpinnerNumberModel tabSizeSpinnerModel = new SpinnerNumberModel(this.initialEditorTabSize,
                Editor.MIN_TAB_SIZE,
                Editor.MAX_TAB_SIZE, 1);
        this.tabSizeSpinSelector = new JSpinner(tabSizeSpinnerModel);
        this.tabSizeSpinSelector.setToolTipText(TAB_SIZE_TOOL_TIP_TEXT);
        this.tabSizeSpinSelector.addChangeListener(
                e -> {
                    final Object value = ((JSpinner) e.getSource()).getValue();
                    this.tabSizeSelector.setValue((Integer) value);
                });

        // highlighting of current line
        this.initialLineHighlighting = Globals.getSettings()
                .getBooleanSetting(Settings.Bool.EDITOR_CURRENT_LINE_HIGHLIGHTING);
        this.lineHighlightCheck = new JCheckBox("Highlight the line currently being edited");
        this.lineHighlightCheck.setSelected(this.initialLineHighlighting);
        this.lineHighlightCheck.setToolTipText(CURRENT_LINE_HIGHLIGHT_TOOL_TIP_TEXT);

        // auto-indent
        this.initialAutoIndent = Globals.getSettings().getBooleanSetting(Settings.Bool.AUTO_INDENT);
        this.autoIndentCheck = new JCheckBox("Auto-Indent");
        this.autoIndentCheck.setSelected(this.initialAutoIndent);
        this.autoIndentCheck.setToolTipText(AUTO_INDENT_TOOL_TIP_TEXT);

        // cursor blink rate selector
        this.initialCaretBlinkRate = Globals.getSettings().getCaretBlinkRate();
        this.blinkSample = new JTextField("     ");
        this.blinkSample.setCaretPosition(2);
        this.blinkSample.setToolTipText(BLINK_SAMPLE_TOOL_TIP_TEXT);
        this.blinkSample.setEnabled(false);
        this.blinkCaret = this.blinkSample.getCaret();
        this.blinkCaret.setBlinkRate(this.initialCaretBlinkRate);
        this.blinkCaret.setVisible(true);
        final SpinnerNumberModel blinkRateSpinnerModel = new SpinnerNumberModel(this.initialCaretBlinkRate,
                Editor.MIN_BLINK_RATE, Editor.MAX_BLINK_RATE, 100);
        this.blinkRateSpinSelector = new JSpinner(blinkRateSpinnerModel);
        this.blinkRateSpinSelector.setToolTipText(BLINK_SPINNER_TOOL_TIP_TEXT);
        this.blinkRateSpinSelector.addChangeListener(
                e -> {
                    final Object value = ((JSpinner) e.getSource()).getValue();
                    this.blinkCaret.setBlinkRate((Integer) value);
                    this.blinkSample.requestFocus();
                    this.blinkCaret.setVisible(true);
                });

        final JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tabPanel.add(new JLabel("Tab Size"));
        tabPanel.add(this.tabSizeSelector);
        tabPanel.add(this.tabSizeSpinSelector);

        final JPanel blinkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blinkPanel.add(new JLabel("Cursor Blinking Rate in ms (0=no blink)"));
        blinkPanel.add(this.blinkRateSpinSelector);
        blinkPanel.add(this.blinkSample);

        otherSettingsPanel.setLayout(new GridLayout(1, 2));
        final JPanel leftColumnSettingsPanel = new JPanel(new GridLayout(4, 1));
        leftColumnSettingsPanel.add(tabPanel);
        leftColumnSettingsPanel.add(blinkPanel);
        leftColumnSettingsPanel.add(this.lineHighlightCheck);
        leftColumnSettingsPanel.add(this.autoIndentCheck);

        // Combine instruction guide off/on and instruction prefix length into radio
        // buttons
        final JPanel rightColumnSettingsPanel = new JPanel(new GridLayout(4, 1));
        final ButtonGroup popupGuidanceButtons = new ButtonGroup();
        this.popupGuidanceOptions = new JRadioButton[3];
        this.popupGuidanceOptions[0] = new JRadioButton("No popup instruction or directive guide");
        this.popupGuidanceOptions[1] = new JRadioButton("Display instruction guide after 1 letter typed");
        this.popupGuidanceOptions[2] = new JRadioButton("Display instruction guide after 2 letters typed");
        for (int i = 0; i < this.popupGuidanceOptions.length; i++) {
            this.popupGuidanceOptions[i].setSelected(false);
            this.popupGuidanceOptions[i].setToolTipText(POPUP_GUIDANCE_TOOL_TIP_TEXT[i]);
            popupGuidanceButtons.add(this.popupGuidanceOptions[i]);
        }
        this.initialPopupGuidance = Globals.getSettings().getBooleanSetting(Settings.Bool.POPUP_INSTRUCTION_GUIDANCE)
                ? Globals.getSettings().getEditorPopupPrefixLength()
                : 0;
        this.popupGuidanceOptions[this.initialPopupGuidance].setSelected(true);
        rightColumnSettingsPanel.setBorder(BorderFactory.createTitledBorder("Popup Instruction Guide"));
        rightColumnSettingsPanel.add(this.popupGuidanceOptions[0]);
        rightColumnSettingsPanel.add(this.popupGuidanceOptions[1]);
        rightColumnSettingsPanel.add(this.popupGuidanceOptions[2]);

        otherSettingsPanel.add(leftColumnSettingsPanel);
        otherSettingsPanel.add(rightColumnSettingsPanel);
        return otherSettingsPanel;
    }

    // Editor style panel
    private @NotNull JPanel buildEditorStylePanel() {
        final JPanel editorStylePanel = new JPanel(new GridLayout(5, 3, gridHGap, gridVGap));
//        this.bgChanger = new ColorChangerPanel("Background", "Select the Editor's Background-Color",
//                Settings.EDITOR_BACKGROUND);
//        this.fgChanger = new ColorChangerPanel("Foreground", "Select the Editor's Foreground-Color",
//                Settings.EDITOR_FOREGROUND);
//        this.lhChanger = new ColorChangerPanel("Line-Highlight", "Select the Editor's Line-Highlight-Color",
//                Settings.EDITOR_LINE_HIGHLIGHT);
//        this.textSelChanger = new ColorChangerPanel("Text-Selection", "Select the Editor's Text-Selection-Color",
//                Settings.EDITOR_SELECTION_COLOR);
//        this.caretChanger = new ColorChangerPanel("Caret", "Select the Editor's Caret-Color",
//                Settings.EDITOR_CARET_COLOR);

        this.bgChanger.addElements(editorStylePanel);
        this.fgChanger.addElements(editorStylePanel);
        this.lhChanger.addElements(editorStylePanel);
        this.textSelChanger.addElements(editorStylePanel);
        this.caretChanger.addElements(editorStylePanel);

        return editorStylePanel;
    }

    // control style (color, plain/italic/bold) for syntax highlighting
    private @NotNull JPanel buildSyntaxStylePanel() {
        final JPanel syntaxStylePanel = new JPanel();
        this.defaultStyles = SyntaxUtilities.getDefaultSyntaxStyles();
        this.initialStyles = SyntaxUtilities.getCurrentSyntaxStyles();
        final var labels = tokenLabels;
        final var sampleText = tokenExamples;
        this.syntaxStylesAction = false;
        // Count the number of actual styles specified
        // create new arrays (no gaps) for grid display, refer to original index
        this.syntaxStyleIndex = new TokenType[labels.size()];
        this.currentStyles = new HashMap<>();
        final String[] someOtherLabel = new String[labels.size()];
        this.samples = new JLabel[labels.size()];
        this.foregroundButtons = new JButton[labels.size()];
        this.bold = new JToggleButton[labels.size()];
        this.italic = new JToggleButton[labels.size()];
        this.useDefault = new JCheckBox[labels.size()];
        final Font genericFont = new JLabel().getFont();
        this.previewFont = new Font(Font.MONOSPACED, Font.PLAIN, genericFont.getSize());// no bold on button text
        final Font boldFont = new Font(Font.SERIF, Font.BOLD, genericFont.getSize());
        final Font italicFont = new Font(Font.SERIF, Font.ITALIC, genericFont.getSize());
        var count = 0;
        // Set all the fixed features. Changeable features set/reset in
        // initializeSyntaxStyleChangeables
        for (final var entry : labels.entrySet()) {
            final var key = entry.getKey();
            this.syntaxStyleIndex[count] = key;

            final var label = new JLabel();
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createLineBorder(Color.black));
            label.setText(sampleText.get(key));
            label.setBackground(Color.WHITE);
            label.setToolTipText(SAMPLE_TOOL_TIP_TEXT);
            this.samples[count] = label;

            final var foregroundButton = new ColorSelectButton();
            foregroundButton.addActionListener(new ForegroundChanger(count, key));
            foregroundButton.setToolTipText(FOREGROUND_TOOL_TIP_TEXT);
            this.foregroundButtons[count] = foregroundButton;

            final BoldItalicChanger boldItalicChanger = new BoldItalicChanger(count, key);

            final var boldButton = new JToggleButton(BOLD_BUTTON_TOOL_TIP_TEXT, false);
            boldButton.setFont(boldFont);
            boldButton.addActionListener(boldItalicChanger);
            boldButton.setToolTipText(BOLD_TOOL_TIP_TEXT);
            this.bold[count] = boldButton;

            final var italicButton = new JToggleButton(ITALIC_BUTTON_TOOL_TIP_TEXT, false);
            italicButton.setFont(italicFont);
            italicButton.addActionListener(boldItalicChanger);
            italicButton.setToolTipText(ITALIC_TOOL_TIP_TEXT);
            this.italic[count] = italicButton;

            someOtherLabel[count] = entry.getValue();
            this.useDefault[count] = new JCheckBox();
            this.useDefault[count].addItemListener(new DefaultChanger(count, key));
            this.useDefault[count].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
            count++;
        }
        this.initializeSyntaxStyleChangeables();
        // build a grid
        syntaxStylePanel.setLayout(new BorderLayout());
        final JPanel labelPreviewPanel = new JPanel(new GridLayout(this.syntaxStyleIndex.length, 2, gridVGap,
                gridHGap));
        final JPanel buttonsPanel = new JPanel(new GridLayout(this.syntaxStyleIndex.length, 4, gridVGap, gridHGap));
        // column 1: label, column 2: preview, column 3: foreground chooser, column 4/5:
        // bold/italic, column 6: default
        for (int i = 0; i < this.syntaxStyleIndex.length; i++) {
            labelPreviewPanel.add(new JLabel(someOtherLabel[i], SwingConstants.RIGHT));
            labelPreviewPanel.add(this.samples[i]);
            buttonsPanel.add(this.foregroundButtons[i]);
            buttonsPanel.add(this.bold[i]);
            buttonsPanel.add(this.italic[i]);
            buttonsPanel.add(this.useDefault[i]);
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
        instructions.add(new JLabel("= use defaults (disables buttons)"));
        labelPreviewPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        syntaxStylePanel.add(instructions, BorderLayout.NORTH);
        syntaxStylePanel.add(labelPreviewPanel, BorderLayout.WEST);
        syntaxStylePanel.add(buttonsPanel, BorderLayout.CENTER);
        return syntaxStylePanel;
    }

    // Set or reset the changeable features of component for syntax style
    private void initializeSyntaxStyleChangeables() {
        for (int count = 0; count < this.samples.length; count++) {
            final TokenType type = this.syntaxStyleIndex[count];
            this.samples[count].setFont(this.previewFont);
            this.samples[count].setForeground(this.initialStyles.get(type).color());
            this.foregroundButtons[count].setBackground(this.initialStyles.get(type).color());
            this.foregroundButtons[count].setEnabled(true);
            this.currentStyles.put(type, this.initialStyles.get(type));
            this.bold[count].setSelected(this.initialStyles.get(type).bold());
            if (this.bold[count].isSelected()) {
                final Font f = this.samples[count].getFont();
                this.samples[count].setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            }
            this.italic[count].setSelected(this.initialStyles.get(type).italic());
            if (this.italic[count].isSelected()) {
                final Font f = this.samples[count].getFont();
                this.samples[count].setFont(f.deriveFont(f.getStyle() ^ Font.ITALIC));
            }
            this.useDefault[count].setSelected(this.initialStyles.get(type).toString().equals(this.defaultStyles.get(type).toString()));
            if (this.useDefault[count].isSelected()) {
                this.foregroundButtons[count].setEnabled(false);
                this.bold[count].setEnabled(false);
                this.italic[count].setEnabled(false);
            }
        }
    }

    // set the foreground color, bold and italic of sample (a JLabel)
    private void setSampleStyles(final JLabel sample, final @NotNull SyntaxStyle style) {
        Font f = this.previewFont;
        if (style.bold()) {
            f = f.deriveFont(f.getStyle() ^ Font.BOLD);
        }
        if (style.italic()) {
            f = f.deriveFont(f.getStyle() ^ Font.ITALIC);
        }
        sample.setFont(f);
        sample.setForeground(style.color());
    }

    /**
     * Panel to change colors via {@link Settings}
     */
    private static class ColorChangerPanel extends JPanel {
        private final int index;
        private final ColorSelectButton colorSelect;
        private final JButton modeButton;
        private final JLabel label;
        private Settings.ColorMode mode;
        private Color color;

        /**
         * Creates a {@link ColorChangerPanel}
         *
         * @param label The label to be put next to the changer
         * @param title The title of the dialogue to be opened
         * @param index the index of the color for
         *              {@link Settings#getColorSettingByPosition(int)} and
         *              {@link Settings#setColorSettingByPosition(int, Color)}
         */
        public ColorChangerPanel(final String label, final String title, final int index) {
            super(new FlowLayout(FlowLayout.LEFT));
            this.index = index;
            this.label = new JLabel(label);
            this.add(this.label);
            this.colorSelect = new ColorSelectButton(); // defined in SettingsHighlightingAction
            this.color = Globals.getSettings().getColorSettingByPosition(index);
            this.mode = Globals.getSettings().getColorModeByPosition(index);
            this.colorSelect.setBackground(this.color);
            this.colorSelect.addActionListener(e -> {
                final Color newColor = JColorChooser.showDialog(null, title, this.colorSelect.getBackground());
                if (newColor != null) {
                    this.color = newColor;
                    this.setMode(Settings.ColorMode.CUSTOM);
                    this.colorSelect.setBackground(this.color);
                }
            });
            this.add(this.colorSelect);

            this.modeButton = new JButton(this.mode.name());
            this.modeButton.addActionListener(e -> this.setMode(this.mode == Settings.ColorMode.DEFAULT ?
                    Settings.ColorMode.SYSTEM
                    : Settings.ColorMode.DEFAULT));
            this.add(this.modeButton);

            this.setToolTipText(title);
        }

        public void reset() {
            this.setMode(Globals.getSettings().getDefaultColorMode());
        }

        public void save() {
            if (this.mode == Settings.ColorMode.CUSTOM) {
                Globals.getSettings().setColorSettingByPosition(this.index, this.colorSelect.getBackground());
            } else {
                Globals.getSettings().setColorSettingByPosition(this.index, this.mode);
            }
            this.updateColorBySetting();
        }

        public void updateColorBySetting() {
            this.color = Globals.getSettings().getColorSettingByPosition(this.index);
            this.colorSelect.setBackground(this.color);
        }

        private void setMode(final Settings.@NotNull ColorMode mode) {
            this.mode = mode;
            this.modeButton.setText(mode.name());
            if (mode != Settings.ColorMode.CUSTOM) {
                this.color = Globals.getSettings().previewColorModeByPosition(this.index, mode);
                this.colorSelect.setBackground(this.color);
            }
        }

        public void addElements(final @NotNull JPanel gridPanel) {
            gridPanel.add(this.label);
            gridPanel.add(this.colorSelect);
            gridPanel.add(this.modeButton);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Toggle bold or italic style on preview button when B or I button clicked
    private class BoldItalicChanger implements ActionListener {
        private final int row;
        private final @NotNull TokenType type;

        public BoldItalicChanger(final int row, final @NotNull TokenType type) {
            this.row = row;
            this.type = type;
        }

        @Override
        public void actionPerformed(final @NotNull ActionEvent e) {
            final Font f = EditorFontDialog.this.samples[this.row].getFont();
            if (e.getActionCommand().equals(BOLD_BUTTON_TOOL_TIP_TEXT)) {
                if (EditorFontDialog.this.bold[this.row].isSelected()) {
                    EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() | Font.BOLD));
                } else {
                    EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
                }
            } else {
                if (EditorFontDialog.this.italic[this.row].isSelected()) {
                    EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() | Font.ITALIC));
                } else {
                    EditorFontDialog.this.samples[this.row].setFont(f.deriveFont(f.getStyle() ^ Font.ITALIC));
                }
            }
            EditorFontDialog.this.currentStyles.put(type,
                    new SyntaxStyle(EditorFontDialog.this.foregroundButtons[this.row].getBackground(),
                            EditorFontDialog.this.italic[this.row].isSelected(),
                            EditorFontDialog.this.bold[this.row].isSelected()));
            EditorFontDialog.this.syntaxStylesAction = true;

        }
    }

    // //////////////////////////////////////////////////////////////
    //
    // Class that handles click on the foreground selection button
    //
    private class ForegroundChanger implements ActionListener {
        private final int row;
        private final @NotNull TokenType type;

        public ForegroundChanger(final int pos, final @NotNull TokenType type) {
            this.row = pos;
            this.type = type;
        }

        @Override
        public void actionPerformed(final @NotNull ActionEvent e) {
            final JButton button = (JButton) e.getSource();
            final Color newColor = JColorChooser.showDialog(null, "Set Text Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                EditorFontDialog.this.samples[this.row].setForeground(newColor);
            }
            currentStyles.put(type, new SyntaxStyle(button.getBackground(),
                    EditorFontDialog.this.italic[this.row].isSelected(),
                    EditorFontDialog.this.bold[this.row].isSelected()));
            EditorFontDialog.this.syntaxStylesAction = true;
        }
    }

    // //////////////////////////////////////////////////////////////
    //
    // Class that handles action (check, uncheck) on the Default checkbox.
    //
    private class DefaultChanger implements ItemListener {
        private final int row;
        private final @NotNull TokenType type;

        public DefaultChanger(final int pos, final @NotNull TokenType type) {
            this.row = pos;
            this.type = type;
        }

        @Override
        public void itemStateChanged(final @NotNull ItemEvent e) {

            // If selected: disable buttons, save current settings, set to defaults
            // If deselected:restore current settings, enable buttons
            if (e.getStateChange() == ItemEvent.SELECTED) {
                EditorFontDialog.this.foregroundButtons[this.row].setEnabled(false);
                EditorFontDialog.this.bold[this.row].setEnabled(false);
                EditorFontDialog.this.italic[this.row].setEnabled(false);
                currentStyles.put(this.type,
                        new SyntaxStyle(EditorFontDialog.this.foregroundButtons[this.row].getBackground(),
                                EditorFontDialog.this.italic[this.row].isSelected(),
                                EditorFontDialog.this.bold[this.row].isSelected()));
                final var defaultStyle = EditorFontDialog.this.defaultStyles.get(this.type);
                EditorFontDialog.this.setSampleStyles(EditorFontDialog.this.samples[this.row], defaultStyle);
                EditorFontDialog.this.foregroundButtons[this.row].setBackground(defaultStyle.color());
                EditorFontDialog.this.bold[this.row].setSelected(defaultStyle.bold());
                EditorFontDialog.this.italic[this.row].setSelected(defaultStyle.italic());
            } else {
                EditorFontDialog.this.setSampleStyles(EditorFontDialog.this.samples[this.row],
                        EditorFontDialog.this.currentStyles.get(this.type));
                EditorFontDialog.this.foregroundButtons[this.row].setBackground(EditorFontDialog.this.currentStyles.get(this.type).color());
                EditorFontDialog.this.bold[this.row].setSelected(EditorFontDialog.this.currentStyles.get(this.type).bold());
                EditorFontDialog.this.italic[this.row].setSelected(EditorFontDialog.this.currentStyles.get(this.type).italic());
                EditorFontDialog.this.foregroundButtons[this.row].setEnabled(true);
                EditorFontDialog.this.bold[this.row].setEnabled(true);
                EditorFontDialog.this.italic[this.row].setEnabled(true);
            }
            EditorFontDialog.this.syntaxStylesAction = true;
        }
    }
}
