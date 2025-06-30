package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import rars.venus.actions.GuiAction;
import rars.venus.editors.TextEditingArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

import static rars.venus.util.ListenerUtilsKt.onWindowClosing;

/**
 * Action for the Edit -> Find/Replace menu item
 */
public final class EditFindReplaceAction extends GuiAction {
    private static final String DIALOG_TITLE = "Find and Replace";
    private static String searchString = "";
    private static boolean caseSensitivity = true;

    public EditFindReplaceAction(
        final String name,
        final Icon icon,
        final String descrip,
        final Integer mnemonic,
        final KeyStroke accel,
        final @NotNull VenusUI gui
    ) {
        super(name, descrip, icon, mnemonic, accel, gui);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        new FindReplaceDialog(this.mainUI).setVisible(true);
    }

    // Private class to do all the work!

    public static final class FindReplaceDialog extends JDialog {
        private static final @NotNull String FIND_TOOL_TIP_TEXT = "Find next occurrence of given text; wraps around at end";
        private static final @NotNull String REPLACE_TOOL_TIP_TEXT = "Replace current occurrence of text then find next";
        private static final @NotNull String REPLACE_ALL_TOOL_TIP_TEXT = "Replace all occurrences of text";
        private static final @NotNull String CLOSE_TOOL_TIP_TEXT = "Close the dialog";
        private static final @NotNull String RESULTS_TOOL_TIP_TEXT = "Outcome of latest operation (button click)";
        private static final @NotNull String RESULTS_TEXT_FOUND = "Text found";
        private static final @NotNull String RESULTS_TEXT_NOT_FOUND = "Text not found";
        private static final @NotNull String RESULTS_TEXT_REPLACED = "Text replaced and found next";
        private static final @NotNull String RESULTS_TEXT_REPLACED_LAST = "Text replaced; last occurrence";
        private static final @NotNull String RESULTS_TEXT_REPLACED_ALL = "Replaced";
        private static final @NotNull String RESULTS_NO_TEXT_TO_FIND = "No text to find";
        private final @NotNull JButton findButton, replaceButton, replaceAllButton, closeButton;
        private final JTextField findInputField, replaceInputField;
        private final JCheckBox caseSensitiveCheckBox;
        private final JLabel resultsLabel;
        @NotNull
        private final VenusUI mainUI;

        public FindReplaceDialog(
            final @NotNull VenusUI mainUI
        ) {
            super(mainUI, EditFindReplaceAction.DIALOG_TITLE, false);
            this.mainUI = mainUI;

            final var dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            this.findInputField = new JTextField(30);
            if (!EditFindReplaceAction.searchString.isEmpty()) {
                this.findInputField.setText(EditFindReplaceAction.searchString);
                this.findInputField.selectAll();
            }
            this.replaceInputField = new JTextField(30);
            dialogPanel.add(buildInputPanel(this.findInputField,
                this.replaceInputField), BorderLayout.NORTH);

            this.caseSensitiveCheckBox = new JCheckBox("Case Sensitive",
                EditFindReplaceAction.caseSensitivity);
            this.resultsLabel = new JLabel("");
            // this.resultsLabel.setForeground(Color.RED);
            this.resultsLabel.setToolTipText(RESULTS_TOOL_TIP_TEXT);
            dialogPanel.add(buildOptionsPanel(caseSensitiveCheckBox,
                resultsLabel));

            this.findButton = new JButton("Find");
            this.findButton.setToolTipText(FIND_TOOL_TIP_TEXT);
            this.replaceButton = new JButton("Replace then Find");
            this.replaceButton.setToolTipText(REPLACE_TOOL_TIP_TEXT);
            this.replaceAllButton = new JButton("Replace all");
            this.replaceAllButton.setToolTipText(REPLACE_ALL_TOOL_TIP_TEXT);
            this.closeButton = new JButton("Close");
            this.closeButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
            dialogPanel.add(
                buildControlPanel(
                    this.findButton,
                    this.replaceButton,
                    this.replaceAllButton,
                    closeButton
                ), BorderLayout.SOUTH
            );

            this.initializeListeners();

            this.setContentPane(dialogPanel);
            this.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
            onWindowClosing(this, e -> {
                performClose();
                return Unit.INSTANCE;
            });
            this.pack();
            this.setLocationRelativeTo(mainUI);
        }

        /**
         * Top part of the dialog, to contain the two input text fields.
         */
        private static @NotNull Component buildInputPanel(
            final @NotNull JTextField findInputField,
            final @NotNull JTextField replaceInputField
        ) {
            final var inputPanel = new JPanel();
            final var labelsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            final var fieldsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            labelsPanel.add(new JLabel("Find what:"));
            labelsPanel.add(new JLabel("Replace with:"));
            fieldsPanel.add(findInputField);
            fieldsPanel.add(replaceInputField);

            final var columns = Box.createHorizontalBox();
            columns.add(labelsPanel);
            columns.add(Box.createHorizontalStrut(6));
            columns.add(fieldsPanel);
            inputPanel.add(columns);
            return inputPanel;
        }

        /**
         * Center part of the dialog, which contains the check box
         * for case sensitivity along with a label to display the
         * outcome of each operation.
         */
        private static @NotNull Component buildOptionsPanel(
            final @NotNull JCheckBox caseSensitiveCheckBox,
            final @NotNull JLabel resultsLabel
        ) {
            final Box optionsPanel = Box.createHorizontalBox();
            final JPanel casePanel = new JPanel(new GridLayout(2, 1));
            casePanel.add(caseSensitiveCheckBox);
            casePanel.setMaximumSize(casePanel.getPreferredSize());
            optionsPanel.add(casePanel);
            optionsPanel.add(Box.createHorizontalStrut(5));
            final JPanel resultsPanel = new JPanel(new GridLayout(1, 1));
            resultsPanel.setBorder(BorderFactory.createTitledBorder("Outcome"));
            resultsPanel.add(resultsLabel);
            optionsPanel.add(resultsPanel);
            return optionsPanel;
        }

        /**
         * Row of control buttons to be placed along the button of the dialog
         */
        private static @NotNull Component buildControlPanel(
            final @NotNull JButton findButton,
            final @NotNull JButton replaceButton,
            final @NotNull JButton replaceAllButton,
            final @NotNull JButton closeButton
        ) {
            final Box controlPanel = Box.createHorizontalBox();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(findButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(replaceButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(replaceAllButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(closeButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        // Private methods to carry out the button actions

        private void initializeListeners() {
            this.findInputField.addActionListener(__ -> this.performFind());
            this.findButton.addActionListener(__ -> this.performFind());
            this.replaceButton.addActionListener(__ -> this.performReplace());
            this.replaceAllButton.addActionListener(__ -> this.performReplaceAll());
            this.closeButton.addActionListener(__ -> this.performClose());
        }

        /**
         * Performs a find. The operation starts at the current cursor position
         * which is not known to this object but is maintained by the EditPane
         * object. The operation will wrap around when it reaches the end of the
         * document. If found, the matching text is selected.
         */
        private void performFind() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                // Being cautious. Should not be null because find/replace tool button disabled
                // if no file open
                final var editorTab = mainUI.mainPane.getCurrentEditTabPane();
                if (editorTab != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final var result = editorTab.getTextArea().doFindText(
                        EditFindReplaceAction.searchString,
                        this.caseSensitiveCheckBox.isSelected()
                    );
                    if (result == TextEditingArea.FindReplaceResult.TEXT_NOT_FOUND) {
                        this.resultsLabel.setText(this.findButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_NOT_FOUND);
                    } else {
                        this.resultsLabel.setText(this.findButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_FOUND);
                    }
                }
            } else {
                this.resultsLabel.setText(this.findButton.getText() + ": " + FindReplaceDialog.RESULTS_NO_TEXT_TO_FIND);
            }
        }

        /**
         * Performs a replace-and-find. If the matched text is current selected with
         * cursor at
         * its end, the replace happens immediately followed by a find for the next
         * occurrence.
         * Otherwise, it performs a find. This will select the matching text so the next
         * press
         * of Replace will do the replace. This is apparently common behavior for
         * replace
         * buttons of different apps I've checked.
         */
        private void performReplace() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                // Being cautious. Should not be null b/c find/replace tool button disabled if
                // no file open
                final var editorTab = mainUI.mainPane.getCurrentEditTabPane();
                if (editorTab != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final var posn = editorTab.getTextArea().doReplace(
                        EditFindReplaceAction.searchString,
                        this.replaceInputField.getText(),
                        this.caseSensitiveCheckBox.isSelected()
                    );
                    String result = this.replaceButton.getText() + ": ";
                    switch (posn) {
                        case TextEditingArea.FindReplaceResult.TEXT_NOT_FOUND:
                            result += FindReplaceDialog.RESULTS_TEXT_NOT_FOUND;
                            break;
                        case TextEditingArea.FindReplaceResult.TEXT_FOUND:
                            result += FindReplaceDialog.RESULTS_TEXT_FOUND;
                            break;
                        case
                            TextEditingArea.FindReplaceResult.TEXT_REPLACED_NOT_FOUND_NEXT:
                            result += FindReplaceDialog.RESULTS_TEXT_REPLACED_LAST;
                            break;
                        case
                            TextEditingArea.FindReplaceResult.TEXT_REPLACED_FOUND_NEXT:
                            result += FindReplaceDialog.RESULTS_TEXT_REPLACED;
                            break;
                    }
                    this.resultsLabel.setText(result);
                }
            } else {
                this.resultsLabel.setText(this.replaceButton.getText() + ": " + FindReplaceDialog.RESULTS_NO_TEXT_TO_FIND);
            }

        }

        /**
         * Performs a replace-all. Makes one pass through the document starting at
         * position 0.
         */
        private void performReplaceAll() {
            this.resultsLabel.setText("");
            if (!this.findInputField.getText().isEmpty()) {
                // Being cautious. Should not be null b/c find/replace tool button disabled if
                // no file open
                final var editorTab = mainUI.mainPane.getCurrentEditTabPane();
                if (editorTab != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final int replaceCount = editorTab.getTextArea()
                        .doReplaceAll(
                            EditFindReplaceAction.searchString,
                            this.replaceInputField.getText(),
                            this.caseSensitiveCheckBox.isSelected()
                        );
                    if (replaceCount == 0) {
                        this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_NOT_FOUND);
                    } else {
                        this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_REPLACED_ALL + ' '
                            + replaceCount + " occurrence" + (
                            replaceCount == 1
                                ? ""
                                : "s"
                        ));
                    }
                }
            } else {
                this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + FindReplaceDialog.RESULTS_NO_TEXT_TO_FIND);
            }
        }

        /**
         * Performs the close operation. Records the current state of the
         * case-sensitivity
         * checkbox into a static variable so it will be remembered across invocations
         * within
         * the session. This also happens with the contents of the "find" text field.
         */
        private void performClose() {
            EditFindReplaceAction.caseSensitivity = this.caseSensitiveCheckBox.isSelected();
            this.setVisible(false);
            this.dispose();
        }
    }

}
