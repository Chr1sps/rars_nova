package rars.venus;

import org.jetbrains.annotations.NotNull;
import rars.venus.editors.TextEditingArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
 * Action for the Edit -> Find/Replace menu item
 */
public final class EditFindReplaceAction extends GuiAction {
    private static final String DIALOG_TITLE = "Find and Replace";
    private static String searchString = "";
    private static boolean caseSensitivity = true;

    public EditFindReplaceAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final @NotNull VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final JDialog findReplaceDialog = new FindReplaceDialog(
            this.mainUI, EditFindReplaceAction.DIALOG_TITLE,
            false
        );
        findReplaceDialog.setVisible(true);
    }

    // Private class to do all the work!

    private class FindReplaceDialog extends JDialog {
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

        public FindReplaceDialog(final Frame owner, final String title, final boolean modality) {
            super(owner, title, modality);

            final var dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            this.findInputField = new JTextField(30);
            if (!EditFindReplaceAction.searchString.isEmpty()) {
                this.findInputField.setText(EditFindReplaceAction.searchString);
                this.findInputField.selectAll();
            }
            this.replaceInputField = new JTextField(30);
            dialogPanel.add(buildInputPanel(this.findInputField, this.replaceInputField), BorderLayout.NORTH);

            this.caseSensitiveCheckBox = new JCheckBox("Case Sensitive", EditFindReplaceAction.caseSensitivity);
            this.resultsLabel = new JLabel("");
            // this.resultsLabel.setForeground(Color.RED);
            this.resultsLabel.setToolTipText(FindReplaceDialog.RESULTS_TOOL_TIP_TEXT);
            dialogPanel.add(buildOptionsPanel(caseSensitiveCheckBox, resultsLabel));

            this.findButton = new JButton("Find");
            this.findButton.setToolTipText(FindReplaceDialog.FIND_TOOL_TIP_TEXT);
            this.replaceButton = new JButton("Replace then Find");
            this.replaceButton.setToolTipText(FindReplaceDialog.REPLACE_TOOL_TIP_TEXT);
            this.replaceAllButton = new JButton("Replace all");
            this.replaceAllButton.setToolTipText(FindReplaceDialog.REPLACE_ALL_TOOL_TIP_TEXT);
            this.closeButton = new JButton("Close");
            this.closeButton.setToolTipText(FindReplaceDialog.CLOSE_TOOL_TIP_TEXT);
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
            this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent we) {
                        FindReplaceDialog.this.performClose();
                    }
                });
            this.pack();
            this.setLocationRelativeTo(owner);
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
            this.findInputField.addActionListener(e -> this.performFind());
            this.findButton.addActionListener(e -> this.performFind());
            this.replaceButton.addActionListener(e -> this.performReplace());
            this.replaceAllButton.addActionListener(e -> this.performReplaceAll());
            this.closeButton.addActionListener(e -> this.performClose());
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
                final EditPane editPane = EditFindReplaceAction.this.mainUI.mainPane.getEditPane();
                if (editPane != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final var posn = editPane.doFindText(
                        EditFindReplaceAction.searchString,
                        this.caseSensitiveCheckBox.isSelected()
                    );
                    if (posn == TextEditingArea.FindReplaceResult.TEXT_NOT_FOUND) {
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
                final EditPane editPane = EditFindReplaceAction.this.mainUI.mainPane.getEditPane();
                if (editPane != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final var posn = editPane.doReplace(
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
                        case TextEditingArea.FindReplaceResult.TEXT_REPLACED_NOT_FOUND_NEXT:
                            result += FindReplaceDialog.RESULTS_TEXT_REPLACED_LAST;
                            break;
                        case TextEditingArea.FindReplaceResult.TEXT_REPLACED_FOUND_NEXT:
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
                final EditPane editPane = EditFindReplaceAction.this.mainUI.mainPane.getEditPane();
                if (editPane != null) {
                    EditFindReplaceAction.searchString = this.findInputField.getText();
                    final int replaceCount = editPane.doReplaceAll(
                        EditFindReplaceAction.searchString,
                        this.replaceInputField.getText(),
                        this.caseSensitiveCheckBox.isSelected()
                    );
                    if (replaceCount == 0) {
                        this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_NOT_FOUND);
                    } else {
                        this.resultsLabel.setText(this.replaceAllButton.getText() + ": " + FindReplaceDialog.RESULTS_TEXT_REPLACED_ALL + " "
                            + replaceCount + " occurrence" + (replaceCount == 1 ? "" : "s"));
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
