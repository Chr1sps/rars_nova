package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.Globals;
import rars.simulator.Simulator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import static rars.Globals.FONT_SETTINGS;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * Creates the message window at the bottom of the UI.
 *
 * @author Team JSpim
 */
public final class MessagesPane extends JTabbedPane {
    // These constants are designed to keep scrolled contents of the
    // two message areas from becoming overwhelmingly large (which
    // seems to slow things down as new text is appended). Once it
    // reaches MAXIMUM_SCROLLED_CHARACTERS in length then cut off
    // the first NUMBER_OF_CHARACTERS_TO_CUT characters. The latter
    // must obviously be smaller than the former.
    public static final int MAXIMUM_SCROLLED_CHARACTERS = Globals.maximumMessageCharacters;
    public static final int NUMBER_OF_CHARACTERS_TO_CUT = Globals.maximumMessageCharacters / 10; // 10%

    private final @NotNull JTextArea assembleTextArea, runTextArea;
    private final @NotNull JPanel assembleTab, runTab;
    @NotNull
    private final VenusUI mainUI;

    /**
     * Constructor for the class, sets up two fresh tabbed text areas for program
     * feedback.
     */
    public MessagesPane(final @NotNull VenusUI mainUI) {
        super();
        this.mainUI = mainUI;
        this.setMinimumSize(new Dimension(0, 0));
        this.assembleTextArea = new JTextArea();
        this.runTextArea = new JTextArea();
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignore -> {
            this.assembleTextArea.setFont(FONT_SETTINGS.getCurrentFont());
            this.runTextArea.setFont(FONT_SETTINGS.getCurrentFont());
        });
        this.assembleTextArea.setEditable(false);
        this.runTextArea.setEditable(false);
        // Set both text areas to mono font. For assemble
        // pane, will make messages more readable. For run
        // pane, will allow properly aligned "text graphics"
        // DPS 15 Dec 2008
        this.assembleTextArea.setFont(FONT_SETTINGS.getCurrentFont());
        this.runTextArea.setFont(FONT_SETTINGS.getCurrentFont());

        final JButton assembleTabClearButton = new JButton("Clear");
        assembleTabClearButton.setToolTipText("Clear the Messages area");
        assembleTabClearButton.addActionListener(
            e -> this.assembleTextArea.setText(""));
        this.assembleTab = new JPanel(new BorderLayout());
        this.assembleTab.add(MessagesPane.createBoxForButton(assembleTabClearButton), BorderLayout.WEST);
        this.assembleTab.add(
            new JScrollPane(
                this.assembleTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            ), BorderLayout.CENTER
        );
        this.assembleTextArea.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    String text;
                    int lineStart = 0;
                    int lineEnd = 0;
                    try {
                        final int line =
                            MessagesPane.this.assembleTextArea.getLineOfOffset(MessagesPane.this.assembleTextArea.viewToModel2D(
                                e.getPoint()));
                        lineStart = MessagesPane.this.assembleTextArea.getLineStartOffset(line);
                        lineEnd = MessagesPane.this.assembleTextArea.getLineEndOffset(line);
                        text = MessagesPane.this.assembleTextArea.getText(lineStart, lineEnd - lineStart);
                    } catch (final BadLocationException ble) {
                        text = "";
                    }
                    if (!text.isEmpty()) {
                        // If error or warning, parse out the line and column number.
                        if (text.startsWith(ErrorList.ERROR_MESSAGE_PREFIX)
                            || text.startsWith(ErrorList.WARNING_MESSAGE_PREFIX)) {
                            MessagesPane.this.assembleTextArea.select(lineStart, lineEnd);
                            MessagesPane.this.assembleTextArea.setSelectionColor(Color.YELLOW);
                            MessagesPane.this.assembleTextArea.setSelectedTextColor(Color.BLACK);
                            MessagesPane.this.assembleTextArea.repaint();
                            final int separatorPosition = text.indexOf(ErrorList.MESSAGE_SEPARATOR);
                            if (separatorPosition >= 0) {
                                text = text.substring(0, separatorPosition);
                            }
                            final String[] stringTokens = text.split("\\s"); // tokenize with whitespace delimiter
                            final String lineToken = ErrorList.LINE_PREFIX.trim();
                            final String columnToken = ErrorList.POSITION_PREFIX.trim();
                            String lineString = "";
                            String columnString = "";
                            for (int i = 0; i < stringTokens.length; i++) {
                                if (stringTokens[i].equals(lineToken) && i < stringTokens.length - 1) {
                                    lineString = stringTokens[i + 1];
                                }
                                if (stringTokens[i].equals(columnToken) && i < stringTokens.length - 1) {
                                    columnString = stringTokens[i + 1];
                                }
                            }
                            int line = 0;
                            try {
                                line = Integer.parseInt(lineString);
                            } catch (final NumberFormatException ignored) {
                            }
                            int column = 0;
                            try {
                                column = Integer.parseInt(columnString);
                            } catch (final NumberFormatException ignored) {
                            }
                            // everything between FILENAME_PREFIX and LINE_PREFIX is file.
                            final int fileNameStart = text.indexOf(ErrorList.FILENAME_PREFIX)
                                + ErrorList.FILENAME_PREFIX.length();
                            final int fileNameEnd = text.indexOf(ErrorList.LINE_PREFIX);
                            String fileName = "";
                            if (fileNameStart < fileNameEnd
                                && fileNameStart >= ErrorList.FILENAME_PREFIX.length()) {
                                fileName = text.substring(fileNameStart, fileNameEnd).trim();
                            }
                            if (!fileName.isEmpty()) {
                                final var file = new File(fileName);
                                MessagesPane.this.mainUI.mainPane.editTabbedPane.selectEditorTextLine(file, line);
                                MessagesPane.this.selectErrorMessage(file, line, column);
                            }
                        }
                    }
                }
            });

        final JButton runTabClearButton = new JButton("Clear");
        runTabClearButton.setToolTipText("Clear the Run I/O area");
        runTabClearButton.addActionListener(
            e -> MessagesPane.this.runTextArea.setText(""));
        this.runTab = new JPanel(new BorderLayout());
        this.runTab.add(MessagesPane.createBoxForButton(runTabClearButton), BorderLayout.WEST);
        this.runTab.add(
            new JScrollPane(
                this.runTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            ), BorderLayout.CENTER
        );

        this.addTab("Messages", this.assembleTab);
        this.addTab("Run I/O", this.runTab);

        this.setToolTipTextAt(
            0,
            "Messages produced by Run menu. Click on assemble error message to select erroneous line"
        );
        this.setToolTipTextAt(1, "Simulated console input and output");
    }

    // Center given button in a box, centered vertically and 6 pixels on left and
    // right
    private static @NotNull Box createBoxForButton(final JButton button) {
        final Box buttonRow = Box.createHorizontalBox();
        buttonRow.add(Box.createHorizontalStrut(6));
        buttonRow.add(button);
        buttonRow.add(Box.createHorizontalStrut(6));
        final Box buttonBox = Box.createVerticalBox();
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(buttonRow);
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    /**
     * Will select the Mars Messages tab error message that matches the given
     * specifications, if it is found. Matching is done by constructing
     * a string using the parameter values and searching the text area for the last
     * occurrance of that string.
     *
     * @param file
     *     A String containing the file path name.
     * @param line
     *     Line number for error message
     * @param column
     *     Column number for error message
     */
    public void selectErrorMessage(final @NotNull File file, final int line, final int column) {
        final var errorReportSubstring = file.getName() + ErrorList.LINE_PREFIX + line
            + ErrorList.POSITION_PREFIX + column;
        final var textPosition = this.assembleTextArea.getText().lastIndexOf(errorReportSubstring);
        if (textPosition >= 0) {
            try {
                final int textLine = this.assembleTextArea.getLineOfOffset(textPosition);
                final int lineStart = this.assembleTextArea.getLineStartOffset(textLine);
                final int lineEnd = this.assembleTextArea.getLineEndOffset(textLine);
                this.assembleTextArea.setSelectionColor(Color.YELLOW);
                this.assembleTextArea.setSelectedTextColor(Color.BLACK);
                this.assembleTextArea.select(lineStart, lineEnd);
                this.assembleTextArea.getCaret().setSelectionVisible(true);
                this.assembleTextArea.repaint();
            } catch (final BadLocationException ble) {
                // If there is a problem, simply skip the selection
            }
        }
    }

    /**
     * Post a message to the assembler display
     *
     * @param message
     *     String to append to assembler display text
     */
    public void postMessage(final String message) {
        this.assembleTextArea.append(message);
        // can do some crude cutting here. If the document gets "very large",
        // let's cut off the oldest text. This will limit scrolling but the limit
        // can be set reasonably high.
        if (this.assembleTextArea.getDocument().getLength() > MessagesPane.MAXIMUM_SCROLLED_CHARACTERS) {
            try {
                this.assembleTextArea.getDocument().remove(0, MessagesPane.NUMBER_OF_CHARACTERS_TO_CUT);
            } catch (final BadLocationException ble) {
                // only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
            }
        }
        this.assembleTextArea.setCaretPosition(this.assembleTextArea.getDocument().getLength());
        this.setSelectedComponent(this.assembleTab);
    }

    /**
     * Post a message to the runtime display
     *
     * @param message
     *     String to append to runtime display text
     */
    // The work of this method is done by "invokeLater" because
    // its JTextArea is maintained by the main event thread
    // but also used, via this method, by the execution thread for
    // "print" syscalls. "invokeLater" schedules the code to be
    // run under the event-processing thread no matter what.
    // DPS, 23 Aug 2005.
    public void postRunMessage(final String message) {
        SwingUtilities.invokeLater(
            () -> {
                this.setSelectedComponent(this.runTab);
                this.runTextArea.append(message);
                // can do some crude cutting here. If the document gets "very large",
                // let's cut off the oldest text. This will limit scrolling but the limit
                // can be set reasonably high.
                if (this.runTextArea.getDocument().getLength() > MessagesPane.MAXIMUM_SCROLLED_CHARACTERS) {
                    try {
                        this.runTextArea.getDocument().remove(0, MessagesPane.NUMBER_OF_CHARACTERS_TO_CUT);
                    } catch (final BadLocationException ble) {
                        // only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                    }
                }
            });
    }

    /**
     * Make the assembler message tab current (up front)
     */
    public void selectMessageTab() {
        this.setSelectedComponent(this.assembleTab);
    }

    /**
     * Make the runtime message tab current (up front)
     */
    public void selectRunMessageTab() {
        this.setSelectedComponent(this.runTab);
    }

    /**
     * Method used by the SystemIO class to get interactive user input
     * requested by a running MIPS program (e.g. syscall #5 to read an
     * integer). SystemIO knows whether simulator is being run at
     * command line by the user, or by the GUI. If run at command line,
     * it gets input from System.in rather than here.
     * <p>
     * This is an overloaded method. This version, with the String parameter,
     * is used to get input from a popup dialog.
     *
     * @param prompt
     *     Prompt to display to the user.
     * @return User input.
     */
    public String getInputStringFromDialog(final String prompt) {
        final boolean lock = Globals.MEMORY_REGISTERS_LOCK.isHeldByCurrentThread();
        if (lock) {
            Globals.MEMORY_REGISTERS_LOCK.unlock();
        }
        final JOptionPane pane = new JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION);
        pane.setWantsInput(true);
        final JDialog dialog = pane.createDialog(this.mainUI, "Keyboard Input");
        dialog.setVisible(true);
        final String input = (String) pane.getInputValue();
        this.postRunMessage(Globals.userInputAlert + input + "\n");
        if (lock) {
            Globals.MEMORY_REGISTERS_LOCK.lock();
        }
        return input;
    }

    /**
     * Method used by the SystemIO class to get interactive user input
     * requested by a running MIPS program (e.g. syscall #5 to read an
     * integer). SystemIO knows whether simulator is being run at
     * command line by the user, or by the GUI. If run at command line,
     * it gets input from System.in rather than here.
     * <p>
     * This is an overloaded method. This version, with the int parameter,
     * is used to get input from the MARS Run I/O window.
     *
     * @param maxLen:
     *     maximum length of input. This method returns when maxLen
     *     characters have been read. Use -1 for no length restrictions.
     * @return User input.
     */
    public String getInputString(final int maxLen) {
        final boolean lock = Globals.MEMORY_REGISTERS_LOCK.isHeldByCurrentThread();
        if (lock) {
            Globals.MEMORY_REGISTERS_LOCK.unlock();
        }
        final var asker = new Asker(maxLen); // Asker defined immediately below.
        final String out = asker.response();
        if (lock) {
            Globals.MEMORY_REGISTERS_LOCK.lock();
        }
        return out;
    }

    // Thread class for obtaining user input in the Run I/O window (MessagesPane)
    // Written by Ricardo Fern�ndez Pascual [rfernandez@ditec.um.es] December 2009.
    private class Asker {
        private final @NotNull ArrayBlockingQueue<String> resultQueue;
        private final int maxLen;
        private final @NotNull DocumentListener listener;
        private final @NotNull NavigationFilter navigationFilter;
        private final @NotNull Consumer<@NotNull Unit> stopListener;
        private int initialPos;

        public Asker(final int maxLen) {
            this.maxLen = maxLen;
            // initialPos will be set in run()
            listener = new DocumentListener() {
                @Override
                public void insertUpdate(final DocumentEvent e) {
                    EventQueue.invokeLater(
                        () -> {
                            try {
                                final var insertedText = e.getDocument().getText(e.getOffset(), e.getLength());
                                final int i = insertedText.indexOf('\n');
                                if (i >= 0) {
                                    final int offset = e.getOffset() + i;
                                    if (offset + 1 == e.getDocument().getLength()) {
                                        Asker.this.returnResponse();
                                    } else {
                                        // remove the '\n' and put it at the end
                                        e.getDocument().remove(offset, 1);
                                        e.getDocument().insertString(e.getDocument().getLength(), "\n", null);
                                        // insertUpdate will be called again, since we have inserted the '\n' at the
                                        // end
                                    }
                                } else if (Asker.this.maxLen >= 0 && e.getDocument()
                                    .getLength() - Asker.this.initialPos >= Asker.this.maxLen) {
                                    Asker.this.returnResponse();
                                }
                            } catch (final BadLocationException ex) {
                                Asker.this.returnResponse();
                            }
                        });
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    EventQueue.invokeLater(
                        () -> {
                            if ((
                                e.getDocument()
                                    .getLength() < Asker.this.initialPos || e.getOffset() < Asker.this.initialPos
                            )
                                && e instanceof UndoableEdit) {
                                ((UndoableEdit) e).undo();
                                MessagesPane.this.runTextArea.setCaretPosition(e.getOffset() + e.getLength());
                            }
                        });
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                }
            };
            navigationFilter = new NavigationFilter() {
                @Override
                public void moveDot(final FilterBypass fb, int dot, final Bias bias) {
                    if (dot < Asker.this.initialPos) {
                        dot = Math.min(Asker.this.initialPos, MessagesPane.this.runTextArea.getDocument().getLength());
                    }
                    fb.moveDot(dot, bias);
                }

                @Override
                public void setDot(final FilterBypass fb, int dot, final Bias bias) {
                    if (dot < Asker.this.initialPos) {
                        dot = Math.min(Asker.this.initialPos, MessagesPane.this.runTextArea.getDocument().getLength());
                    }
                    fb.setDot(dot, bias);
                }
            };
            stopListener = ignored -> Asker.this.returnResponse();
            resultQueue = new ArrayBlockingQueue<>(1);
        }

        private void run() { // must be invoked from the GUI thread
            MessagesPane.this.selectRunMessageTab();
            MessagesPane.this.runTextArea.setEditable(true);
            MessagesPane.this.runTextArea.requestFocusInWindow();
            MessagesPane.this.runTextArea.setCaretPosition(MessagesPane.this.runTextArea.getDocument().getLength());
            this.initialPos = MessagesPane.this.runTextArea.getCaretPosition();
            MessagesPane.this.runTextArea.setNavigationFilter(this.navigationFilter);
            MessagesPane.this.runTextArea.getDocument().addDocumentListener(this.listener);
            final Simulator self = Globals.SIMULATOR;
            self.stopEventHook.subscribe(this.stopListener);
        }

        private void cleanup() { // not required to be called from the GUI thread
            EventQueue.invokeLater(
                () -> {
                    MessagesPane.this.runTextArea.getDocument().removeDocumentListener(Asker.this.listener);
                    MessagesPane.this.runTextArea.setEditable(false);
                    MessagesPane.this.runTextArea.setNavigationFilter(null);
                    MessagesPane.this.runTextArea.setCaretPosition(MessagesPane.this.runTextArea.getDocument()
                        .getLength());
                    final Simulator self = Globals.SIMULATOR;
                    self.stopEventHook.unsubscribe(this.stopListener);
                });
        }

        private void returnResponse() {
            try {
                final int p = Math.min(this.initialPos, MessagesPane.this.runTextArea.getDocument().getLength());
                final int l = Math.min(
                    MessagesPane.this.runTextArea.getDocument().getLength() - p, this.maxLen >= 0 ?
                        this.maxLen : Integer.MAX_VALUE
                );
                this.resultQueue.offer(MessagesPane.this.runTextArea.getText(p, l));
            } catch (final BadLocationException ex) {
                // this cannot happen
                this.resultQueue.offer("");
            }
        }

        private @Nullable String response() {
            EventQueue.invokeLater(this::run);
            try {
                return this.resultQueue.take();
            } catch (final InterruptedException ex) {
                return null;
            } finally {
                this.cleanup();
            }
        }
    } // Asker class
}
