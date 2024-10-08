/*
 * InputHandler.java - Manages first bindings and executes actions
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;

/**
 * An input handler converts the user's first strokes into concrete actions.
 * It also takes care of macro recording and action repetition.
 * <p>
 * <p>
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any first binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov
 * @version $Id: InputHandler.java,v 1.14 1999/12/13 03:40:30 sp Exp $
 * <p>
 * 08/12/2002 Clipboard actions (Oliver Henning)
 */
public abstract class InputHandler extends KeyAdapter {
    /**
     * If this client property is set to Boolean.TRUE on the text area,
     * the home/end keys will support 'smart' BRIEF-like behaviour
     * (one press = start/end of line, two presses = start/end of
     * viewscreen, three presses = start/end of document). By default,
     * this property is not set.
     */
    public static final String SMART_HOME_END_PROPERTY = "InputHandler.homeEnd";
    /**
     * Constant <code>BACKSPACE</code>
     */
    public static final ActionListener BACKSPACE = new backspace();
    /**
     * Constant <code>BACKSPACE_WORD</code>
     */
    public static final ActionListener BACKSPACE_WORD = new backspace_word();
    /**
     * Constant <code>DELETE</code>
     */
    public static final ActionListener DELETE = new delete();
    /**
     * Constant <code>DELETE_WORD</code>
     */
    public static final ActionListener DELETE_WORD = new delete_word();
    /**
     * Constant <code>END</code>
     */
    public static final ActionListener END = new end(false);
    /**
     * Constant <code>DOCUMENT_END</code>
     */
    public static final ActionListener DOCUMENT_END = new document_end(false);
    /**
     * Constant <code>SELECT_ALL</code>
     */
    public static final ActionListener SELECT_ALL = new select_all();
    /**
     * Constant <code>SELECT_END</code>
     */
    public static final ActionListener SELECT_END = new end(true);
    /**
     * Constant <code>SELECT_DOC_END</code>
     */
    public static final ActionListener SELECT_DOC_END = new document_end(true);
    /**
     * Constant <code>INSERT_BREAK</code>
     */
    public static final ActionListener INSERT_BREAK = new insert_break();
    /**
     * Constant <code>INSERT_TAB</code>
     */
    public static final ActionListener INSERT_TAB = new insert_tab();
    /**
     * Constant <code>DELETE_TAB</code>
     */
    public static final ActionListener DELETE_TAB = new delete_tab();
    /**
     * Constant <code>HOME</code>
     */
    public static final ActionListener HOME = new home(false);
    /**
     * Constant <code>DOCUMENT_HOME</code>
     */
    public static final ActionListener DOCUMENT_HOME = new document_home(false);
    /**
     * Constant <code>SELECT_HOME</code>
     */
    public static final ActionListener SELECT_HOME = new home(true);
    /**
     * Constant <code>SELECT_DOC_HOME</code>
     */
    public static final ActionListener SELECT_DOC_HOME = new document_home(true);
    /**
     * Constant <code>NEXT_CHAR</code>
     */
    public static final ActionListener NEXT_CHAR = new next_char(false);
    /**
     * Constant <code>NEXT_LINE</code>
     */
    public static final ActionListener NEXT_LINE = new next_line(false);
    /**
     * Constant <code>NEXT_PAGE</code>
     */
    public static final ActionListener NEXT_PAGE = new next_page(false);
    /**
     * Constant <code>NEXT_WORD</code>
     */
    public static final ActionListener NEXT_WORD = new next_word(false);
    /**
     * Constant <code>SELECT_NEXT_CHAR</code>
     */
    public static final ActionListener SELECT_NEXT_CHAR = new next_char(true);
    /**
     * Constant <code>SELECT_NEXT_LINE</code>
     */
    public static final ActionListener SELECT_NEXT_LINE = new next_line(true);
    /**
     * Constant <code>SELECT_NEXT_PAGE</code>
     */
    public static final ActionListener SELECT_NEXT_PAGE = new next_page(true);
    /**
     * Constant <code>SELECT_NEXT_WORD</code>
     */
    public static final ActionListener SELECT_NEXT_WORD = new next_word(true);
    /**
     * Constant <code>OVERWRITE</code>
     */
    public static final ActionListener OVERWRITE = new overwrite();
    /**
     * Constant <code>PREV_CHAR</code>
     */
    public static final ActionListener PREV_CHAR = new prev_char(false);
    /**
     * Constant <code>PREV_LINE</code>
     */
    public static final ActionListener PREV_LINE = new prev_line(false);
    /**
     * Constant <code>PREV_PAGE</code>
     */
    public static final ActionListener PREV_PAGE = new prev_page(false);
    /**
     * Constant <code>PREV_WORD</code>
     */
    public static final ActionListener PREV_WORD = new prev_word(false);
    /**
     * Constant <code>SELECT_PREV_CHAR</code>
     */
    public static final ActionListener SELECT_PREV_CHAR = new prev_char(true);
    /**
     * Constant <code>SELECT_PREV_LINE</code>
     */
    public static final ActionListener SELECT_PREV_LINE = new prev_line(true);
    /**
     * Constant <code>SELECT_PREV_PAGE</code>
     */
    public static final ActionListener SELECT_PREV_PAGE = new prev_page(true);
    /**
     * Constant <code>SELECT_PREV_WORD</code>
     */
    public static final ActionListener SELECT_PREV_WORD = new prev_word(true);
    /**
     * Constant <code>REPEAT</code>
     */
    public static final ActionListener REPEAT = new repeat();
    /**
     * Constant <code>TOGGLE_RECT</code>
     */
    public static final ActionListener TOGGLE_RECT = new toggle_rect();
    /**
     * Constant <code>CLIP_COPY</code>
     */
    public static final ActionListener CLIP_COPY = new clip_copy();
    // Clipboard
    /**
     * Constant <code>CLIP_PASTE</code>
     */
    public static final ActionListener CLIP_PASTE = new clip_paste();
    /**
     * Constant <code>CLIP_CUT</code>
     */
    public static final ActionListener CLIP_CUT = new clip_cut();
    /**
     * Constant <code>INSERT_CHAR</code>
     */
    public static final ActionListener INSERT_CHAR = new insert_char();

    // Default action
    private static final Logger LOGGER = LogManager.getLogger(InputHandler.class);
    private static final Hashtable<String, ActionListener> actions;

    static {
        actions = new Hashtable<>();
        InputHandler.actions.put("backspace", InputHandler.BACKSPACE);
        InputHandler.actions.put("backspace-word", InputHandler.BACKSPACE_WORD);
        InputHandler.actions.put("delete", InputHandler.DELETE);
        InputHandler.actions.put("delete-word", InputHandler.DELETE_WORD);
        InputHandler.actions.put("end", InputHandler.END);
        InputHandler.actions.put("select-all", InputHandler.SELECT_ALL);
        InputHandler.actions.put("select-end", InputHandler.SELECT_END);
        InputHandler.actions.put("document-end", InputHandler.DOCUMENT_END);
        InputHandler.actions.put("select-doc-end", InputHandler.SELECT_DOC_END);
        InputHandler.actions.put("insert-break", InputHandler.INSERT_BREAK);
        InputHandler.actions.put("insert-tab", InputHandler.INSERT_TAB);
        InputHandler.actions.put("delete-tab", InputHandler.DELETE_TAB);
        InputHandler.actions.put("home", InputHandler.HOME);
        InputHandler.actions.put("select-home", InputHandler.SELECT_HOME);
        InputHandler.actions.put("document-home", InputHandler.DOCUMENT_HOME);
        InputHandler.actions.put("select-doc-home", InputHandler.SELECT_DOC_HOME);
        InputHandler.actions.put("next-char", InputHandler.NEXT_CHAR);
        InputHandler.actions.put("next-line", InputHandler.NEXT_LINE);
        InputHandler.actions.put("next-page", InputHandler.NEXT_PAGE);
        InputHandler.actions.put("next-word", InputHandler.NEXT_WORD);
        InputHandler.actions.put("select-next-char", InputHandler.SELECT_NEXT_CHAR);
        InputHandler.actions.put("select-next-line", InputHandler.SELECT_NEXT_LINE);
        InputHandler.actions.put("select-next-page", InputHandler.SELECT_NEXT_PAGE);
        InputHandler.actions.put("select-next-word", InputHandler.SELECT_NEXT_WORD);
        InputHandler.actions.put("overwrite", InputHandler.OVERWRITE);
        InputHandler.actions.put("prev-char", InputHandler.PREV_CHAR);
        InputHandler.actions.put("prev-line", InputHandler.PREV_LINE);
        InputHandler.actions.put("prev-page", InputHandler.PREV_PAGE);
        InputHandler.actions.put("prev-word", InputHandler.PREV_WORD);
        InputHandler.actions.put("select-prev-char", InputHandler.SELECT_PREV_CHAR);
        InputHandler.actions.put("select-prev-line", InputHandler.SELECT_PREV_LINE);
        InputHandler.actions.put("select-prev-page", InputHandler.SELECT_PREV_PAGE);
        InputHandler.actions.put("select-prev-word", InputHandler.SELECT_PREV_WORD);
        InputHandler.actions.put("repeat", InputHandler.REPEAT);
        InputHandler.actions.put("toggle-rect", InputHandler.TOGGLE_RECT);
        InputHandler.actions.put("insert-char", InputHandler.INSERT_CHAR);
        InputHandler.actions.put("clipboard-copy", InputHandler.CLIP_COPY);
        InputHandler.actions.put("clipboard-paste", InputHandler.CLIP_PASTE);
        InputHandler.actions.put("clipboard-cut", InputHandler.CLIP_CUT);
    }

    // protected members
    protected ActionListener grabAction;
    protected boolean repeat;
    protected int repeatCount;
    protected InputHandler.MacroRecorder recorder;

    /**
     * Returns a named text area action.
     *
     * @param name The action name
     * @return a {@link java.awt.event.ActionListener} object
     */
    public static ActionListener getAction(final String name) {
        return InputHandler.actions.get(name);
    }

    /**
     * Returns the name of the specified text area action.
     *
     * @param listener The action
     * @return a {@link java.lang.String} object
     */
    public static @Nullable String getActionName(final ActionListener listener) {
        final Enumeration<String> enumeration = InputHandler.getActions();
        while (enumeration.hasMoreElements()) {
            final String name = enumeration.nextElement();
            final ActionListener _listener = InputHandler.getAction(name);
            if (_listener == listener)
                return name;
        }
        return null;
    }

    /**
     * Returns an enumeration of all available actions.
     *
     * @return a {@link java.util.Enumeration} object
     */
    public static Enumeration<String> getActions() {
        return InputHandler.actions.keys();
    }

    /**
     * Returns the text area that fired the specified event.
     *
     * @param evt The event
     * @return a {@link JEditTextArea} object
     */
    public static @Nullable JEditTextArea getTextArea(final EventObject evt) {
        if (evt != null) {
            final Object o = evt.getSource();
            if (o instanceof Component c) {
                // find the parent text area
                label:
                for (; ; ) {
                    switch (c) {
                        case final JEditTextArea jEditTextArea:
                            return jEditTextArea;
                        case null:
                            break label;
                        case final JPopupMenu jPopupMenu:
                            c = jPopupMenu
                                    .getInvoker();
                            break;
                        default:
                            c = c.getParent();
                            break;
                    }
                }
            }
        }

        // this shouldn't happen
        InputHandler.LOGGER.error("BUG: getTextArea() returning null");
        InputHandler.LOGGER.error("Report this to Slava Pestov <sp@gjt.org>");
        return null;
    }

    /**
     * Adds the default first bindings to this input handler.
     * This should not be called in the constructor of this
     * input handler, because applications might load the
     * first bindings from a file, etc.
     */
    public abstract void addDefaultKeyBindings();

    /**
     * Adds a first binding to this input handler.
     *
     * @param keyBinding The first binding (the format of this is
     *                   input-handler specific)
     * @param action     The action
     */
    public abstract void addKeyBinding(String keyBinding, ActionListener action);

    /**
     * Removes a first binding from this input handler.
     *
     * @param keyBinding The first binding
     */
    public abstract void removeKeyBinding(String keyBinding);

    /**
     * Removes all first bindings from this input handler.
     */
    public abstract void removeAllKeyBindings();

    /**
     * Grabs the next first typed event and invokes the specified
     * action with the first as a the action command.
     *
     * @param listener the actionlistener
     */
    public void grabNextKeyStroke(final ActionListener listener) {
        this.grabAction = listener;
    }

    /**
     * Returns if repeating is enabled. When repeating is enabled,
     * actions will be executed multiple times. This is usually
     * invoked with a special first stroke in the input handler.
     *
     * @return a boolean
     */
    public boolean isRepeatEnabled() {
        return this.repeat;
    }

    /**
     * Enables repeating. When repeating is enabled, actions will be
     * executed multiple times. Once repeating is enabled, the input
     * handler should read a number from the keyboard.
     *
     * @param repeat a boolean
     */
    public void setRepeatEnabled(final boolean repeat) {
        this.repeat = repeat;
    }

    /**
     * Returns the number of times the next action will be repeated.
     *
     * @return a int
     */
    public int getRepeatCount() {
        return (this.repeat ? Math.max(1, this.repeatCount) : 1);
    }

    /**
     * Sets the number of times the next action will be repeated.
     *
     * @param repeatCount The repeat count
     */
    public void setRepeatCount(final int repeatCount) {
        this.repeatCount = repeatCount;
    }

    // protected members

    /**
     * Returns the macro recorder. If this is non-null, all executed
     * actions should be forwarded to the recorder.
     *
     * @return a {@link InputHandler.MacroRecorder} object
     */
    public InputHandler.MacroRecorder getMacroRecorder() {
        return this.recorder;
    }

    /**
     * Sets the macro recorder. If this is non-null, all executed
     * actions should be forwarded to the recorder.
     *
     * @param recorder The macro recorder
     */
    public void setMacroRecorder(final InputHandler.MacroRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Returns a copy of this input handler that shares the same
     * first bindings. Setting first bindings in the copy will also
     * set them in the original.
     *
     * @return a {@link InputHandler} object
     */
    public abstract InputHandler copy();

    /**
     * Executes the specified action, repeating and recording it as
     * necessary.
     *
     * @param listener      The action listener
     * @param source        The event source
     * @param actionCommand The action command
     */
    public void executeAction(final ActionListener listener, final Object source,
                              final String actionCommand) {
        // create event
        final ActionEvent evt = new ActionEvent(source,
                ActionEvent.ACTION_PERFORMED,
                actionCommand);

        // don't do anything if the action is a wrapper
        // (like EditAction.Wrapper)
        if (listener instanceof Wrapper) {
            listener.actionPerformed(evt);
            return;
        }

        // remember old values, in case action changes them
        final boolean _repeat = this.repeat;
        final int _repeatCount = this.getRepeatCount();

        // execute the action
        if (listener instanceof InputHandler.NonRepeatable) {
            listener.actionPerformed(evt);
        } else {
            for (int i = 0; i < Math.max(1, this.repeatCount); i++)
                listener.actionPerformed(evt);
        }

        // do recording. Notice that we do no recording whatsoever
        // for actions that grab keys
        if (this.grabAction == null) {
            if (this.recorder != null) {
                if (!(listener instanceof InputHandler.NonRecordable)) {
                    if (_repeatCount != 1)
                        this.recorder.actionPerformed(InputHandler.REPEAT, String.valueOf(_repeatCount));

                    this.recorder.actionPerformed(listener, actionCommand);
                }
            }

            // If repeat was true originally, clear it
            // Otherwise it might have been set by the action, etc
            if (_repeat) {
                this.repeat = false;
                this.repeatCount = 0;
            }
        }
    }

    /**
     * If a first is being grabbed, this method should be called with
     * the appropriate first event. It executes the grab action with
     * the typed character as the parameter.
     *
     * @param evt a {@link java.awt.event.KeyEvent} object
     */
    protected void handleGrabAction(final KeyEvent evt) {
        // Clear it *before* it is executed so that executeAction()
        // resets the repeat count
        final ActionListener _grabAction = this.grabAction;
        this.grabAction = null;
        this.executeAction(_grabAction, evt.getSource(),
                String.valueOf(evt.getKeyChar()));
    }

    /**
     * If an action implements this interface, it should not be repeated.
     * Instead, it will handle the repetition itself.
     */
    public interface NonRepeatable {
    }

    /**
     * If an action implements this interface, it should not be recorded
     * by the macro recorder. Instead, it will do its own recording.
     */
    public interface NonRecordable {
    }

    /**
     * For use by EditAction.Wrapper only.
     *
     * @since jEdit 2.2final
     */
    public interface Wrapper {
    }

    /**
     * Macro recorder.
     */
    public interface MacroRecorder {
        void actionPerformed(ActionListener listener,
                             String actionCommand);
    }

    public static class backspace implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            } else {
                final int caret = textArea.getCaretPosition();
                if (caret == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                try {
                    textArea.getDocument().remove(caret - 1, 1);
                } catch (final BadLocationException bl) {
                    InputHandler.LOGGER.error("Bad location exception", bl);
                }
            }
        }
    }

    public static class backspace_word implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final int start = textArea.getSelectionStart();
            if (start != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }

            final int line = textArea.getCaretLine();
            final int lineStart = textArea.getLineStartOffset(line);
            int caret = start - lineStart;

            final String lineText = textArea.getLineText(textArea
                    .getCaretLine());

            if (caret == 0) {
                if (lineStart == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret--;
            } else {
                final String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
            }

            try {
                textArea.getDocument().remove(
                        caret + lineStart,
                        start - (caret + lineStart));
            } catch (final BadLocationException bl) {
                InputHandler.LOGGER.error("Bad location exception", bl);
            }
        }
    }

    public static class delete implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            } else {
                final int caret = textArea.getCaretPosition();
                if (caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                try {
                    textArea.getDocument().remove(caret, 1);
                } catch (final BadLocationException bl) {
                    InputHandler.LOGGER.error("Bad location exception", bl);
                }
            }
        }
    }

    public static class delete_word implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final int start = textArea.getSelectionStart();
            if (start != textArea.getSelectionEnd()) {
                textArea.setSelectedText("");
            }

            final int line = textArea.getCaretLine();
            final int lineStart = textArea.getLineStartOffset(line);
            int caret = start - lineStart;

            final String lineText = textArea.getLineText(textArea
                    .getCaretLine());

            if (caret == lineText.length()) {
                if (lineStart + caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret++;
            } else {
                final String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
            }

            try {
                textArea.getDocument().remove(start,
                        (caret + lineStart) - start);
            } catch (final BadLocationException bl) {
                InputHandler.LOGGER.error("Bad location exception", bl);
            }
        }
    }

    public static class end implements ActionListener {
        private final boolean select;

        public end(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            int caret = textArea.getCaretPosition();

            final int lastOfLine = textArea.getLineEndOffset(
                    textArea.getCaretLine()) - 1;
            int lastVisibleLine = textArea.getFirstLine()
                    + textArea.getVisibleLines();
            if (lastVisibleLine >= textArea.getLineCount()) {
                lastVisibleLine = Math.min(textArea.getLineCount() - 1,
                        lastVisibleLine);
            } else
                lastVisibleLine -= (textArea.getElectricScroll() + 1);

            final int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
            final int lastDocument = textArea.getDocumentLength();

            if (caret == lastDocument) {
                textArea.getToolkit().beep();
                return;
            } else if (!Boolean.TRUE.equals(textArea.getClientProperty(
                    InputHandler.SMART_HOME_END_PROPERTY)))
                caret = lastOfLine;
            else if (caret == lastVisible)
                caret = lastDocument;
            else if (caret == lastOfLine)
                caret = lastVisible;
            else
                caret = lastOfLine;

            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
        }
    }

    public static class select_all implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.selectAll();
        }
    }

    public static class document_end implements ActionListener {
        private final boolean select;

        public document_end(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            if (this.select)
                textArea.select(textArea.getMarkPosition(),
                        textArea.getDocumentLength());
            else
                textArea.setCaretPosition(textArea
                        .getDocumentLength());
        }
    }

    public static class home implements ActionListener {
        private final boolean select;

        public home(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            int caret = textArea.getCaretPosition();

            final int firstLine = textArea.getFirstLine();

            final int firstOfLine = textArea.getLineStartOffset(
                    textArea.getCaretLine());
            final int firstVisibleLine = (firstLine == 0 ? 0 : firstLine + textArea.getElectricScroll());
            final int firstVisible = textArea.getLineStartOffset(
                    firstVisibleLine);

            if (caret == 0) {
                textArea.getToolkit().beep();
                return;
            } else if (!Boolean.TRUE.equals(textArea.getClientProperty(
                    InputHandler.SMART_HOME_END_PROPERTY)))
                caret = firstOfLine;
            else if (caret == firstVisible)
                caret = 0;
            else if (caret == firstOfLine)
                caret = firstVisible;
            else
                caret = firstOfLine;

            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
        }
    }

    public static class document_home implements ActionListener {
        private final boolean select;

        public document_home(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            if (this.select)
                textArea.select(textArea.getMarkPosition(), 0);
            else
                textArea.setCaretPosition(0);
        }
    }

    public static class insert_break implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }
            // AutoIndent feature added DPS 31-Dec-2010
            textArea.setSelectedText("\n" + textArea.getAutoIndent());
        }
    }

    public static class insert_tab implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            final int startOffset = textArea.getSelectionStart();
            final int startLine = textArea.getSelectionStartLine();
            final int startLineOffset = textArea.getLineStartOffset(startLine);
            final int endOffset = textArea.getSelectionEnd();
            final int endLine = textArea.getSelectionEndLine();
            final int endLineOffset = textArea.getLineEndOffset(endLine);

            if (startLineOffset != endLineOffset && startLine != endLine) {
                final String text = textArea.getText();
                final String selected = text.substring(startLineOffset, endLineOffset - 1);
                final String prefixed = TextUtilities.addLinePrefixes(selected, "\t");

                try {
                    textArea.document.replace(startLineOffset, endLineOffset - startLineOffset - 1, prefixed, null);
                } catch (final BadLocationException bl) {
                    InputHandler.LOGGER.error("Bad location exception", bl);
                }
                textArea.select(startOffset + 1, endOffset + (prefixed.length() - selected.length()));
            } else {
                textArea.overwriteSetSelectedText("\t");
            }
        }
    }

    public static class delete_tab implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);

            if (!textArea.isEditable()) {
                textArea.getToolkit().beep();
                return;
            }

            final int startOffset = textArea.getSelectionStart();
            final int startLine = textArea.getSelectionStartLine();
            final int startLineOffset = textArea.getLineStartOffset(startLine);
            final int endOffset = textArea.getSelectionEnd();
            final int endLine = textArea.getSelectionEndLine();
            final int endLineOffset = textArea.getLineEndOffset(endLine);

            if (startOffset != endOffset) {
                final String text = textArea.getText();
                final String selected = text.substring(startLineOffset, endLineOffset - 1);
                final String stripped = TextUtilities.deleteLinePrefixes(selected, "\t");

                if (selected.equals(stripped)) {
                    textArea.getToolkit().beep();
                    return;
                }

                try {
                    textArea.document.replace(startLineOffset, endLineOffset - startLineOffset - 1, stripped, null);
                } catch (final BadLocationException bl) {
                    InputHandler.LOGGER.error("Bad location exception", bl);
                }

                textArea.select(Math.max(startOffset - 1, 0),
                        endOffset + (stripped.length() - selected.length()));
            } else {
                final int caretOffset = textArea.getCaretPosition();
                final int caretLine = textArea.getCaretLine();
                final int caretLineOffset = textArea.getLineStartOffset(caretLine);

                if (caretOffset == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                try {
                    final String lineText = textArea.getLineText(caretLine);
                    int tabLineIndex = -1;
                    for (int i = 0; i < lineText.length(); i++) {
                        if (lineText.charAt(i) != '\t') {
                            break;
                        }
                        tabLineIndex = i;
                    }

                    if (tabLineIndex == -1) {
                        textArea.getToolkit().beep();
                        return;
                    }

                    final int tabIndex = caretLineOffset + tabLineIndex;
                    textArea.getDocument().remove(tabIndex, 1);
                } catch (final BadLocationException bl) {
                    InputHandler.LOGGER.error("Bad location exception", bl);
                }
            }
        }
    }

    public static class next_char implements ActionListener {
        private final boolean select;

        public next_char(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final int caret = textArea.getCaretPosition();
            if (caret == textArea.getDocumentLength()) {
                textArea.getToolkit().beep();
                return;
            }

            if (this.select)
                textArea.select(textArea.getMarkPosition(),
                        caret + 1);
            else
                textArea.setCaretPosition(caret + 1);
        }
    }

    public static class next_line implements ActionListener {
        private final boolean select;

        public next_line(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            int caret = textArea.getCaretPosition();
            final int line = textArea.getCaretLine();

            if (line == textArea.getLineCount() - 1) {
                textArea.getToolkit().beep();
                return;
            }

            int magic = textArea.getMagicCaretPosition();
            if (magic == -1) {
                magic = textArea.offsetToX(line,
                        caret - textArea.getLineStartOffset(line));
            }

            caret = textArea.getLineStartOffset(line + 1)
                    + textArea.xToOffset(line + 1, magic);
            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
            textArea.setMagicCaretPosition(magic);
        }
    }

    public static class next_page implements ActionListener {
        private final boolean select;

        public next_page(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final int lineCount = textArea.getLineCount();
            int firstLine = textArea.getFirstLine();
            final int visibleLines = textArea.getVisibleLines();
            final int line = textArea.getCaretLine();

            firstLine += visibleLines;

            if (firstLine + visibleLines >= lineCount - 1)
                firstLine = lineCount - visibleLines;

            textArea.setFirstLine(firstLine);

            final int caret = textArea.getLineStartOffset(
                    Math.min(textArea.getLineCount() - 1,
                            line + visibleLines));
            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
        }
    }

    public static class next_word implements ActionListener {
        private final boolean select;

        public next_word(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            int caret = textArea.getCaretPosition();
            final int line = textArea.getCaretLine();
            final int lineStart = textArea.getLineStartOffset(line);
            caret -= lineStart;

            final String lineText = textArea.getLineText(textArea
                    .getCaretLine());

            if (caret == lineText.length()) {
                if (lineStart + caret == textArea.getDocumentLength()) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret++;
            } else {
                final String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
            }

            if (this.select)
                textArea.select(textArea.getMarkPosition(),
                        lineStart + caret);
            else
                textArea.setCaretPosition(lineStart + caret);
        }
    }

    public static class overwrite implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.setOverwriteEnabled(
                    !textArea.isOverwriteEnabled());
        }
    }

    public static class prev_char implements ActionListener {
        private final boolean select;

        public prev_char(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final int caret = textArea.getCaretPosition();
            if (caret == 0) {
                textArea.getToolkit().beep();
                return;
            }

            if (this.select)
                textArea.select(textArea.getMarkPosition(),
                        caret - 1);
            else
                textArea.setCaretPosition(caret - 1);
        }
    }

    public static class prev_line implements ActionListener {
        private final boolean select;

        public prev_line(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            int caret = textArea.getCaretPosition();
            final int line = textArea.getCaretLine();

            if (line == 0) {
                textArea.getToolkit().beep();
                return;
            }

            int magic = textArea.getMagicCaretPosition();
            if (magic == -1) {
                magic = textArea.offsetToX(line,
                        caret - textArea.getLineStartOffset(line));
            }

            caret = textArea.getLineStartOffset(line - 1)
                    + textArea.xToOffset(line - 1, magic);
            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
            textArea.setMagicCaretPosition(magic);
        }
    }

    public static class prev_page implements ActionListener {
        private final boolean select;

        public prev_page(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            int firstLine = textArea.getFirstLine();
            final int visibleLines = textArea.getVisibleLines();
            final int line = textArea.getCaretLine();

            if (firstLine < visibleLines)
                firstLine = visibleLines;

            textArea.setFirstLine(firstLine - visibleLines);

            final int caret = textArea.getLineStartOffset(
                    Math.max(0, line - visibleLines));
            if (this.select)
                textArea.select(textArea.getMarkPosition(), caret);
            else
                textArea.setCaretPosition(caret);
        }
    }

    public static class prev_word implements ActionListener {
        private final boolean select;

        public prev_word(final boolean select) {
            this.select = select;
        }

        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            int caret = textArea.getCaretPosition();
            final int line = textArea.getCaretLine();
            final int lineStart = textArea.getLineStartOffset(line);
            caret -= lineStart;

            final String lineText = textArea.getLineText(textArea
                    .getCaretLine());

            if (caret == 0) {
                if (lineStart == 0) {
                    textArea.getToolkit().beep();
                    return;
                }
                caret--;
            } else {
                final String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
                caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
            }

            if (this.select)
                textArea.select(textArea.getMarkPosition(),
                        lineStart + caret);
            else
                textArea.setCaretPosition(lineStart + caret);
        }
    }

    public static class repeat implements ActionListener,
            InputHandler.NonRecordable {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.getInputHandler().setRepeatEnabled(true);
            final String actionCommand = evt.getActionCommand();
            if (actionCommand != null) {
                textArea.getInputHandler().setRepeatCount(
                        Integer.parseInt(actionCommand));
            }
        }
    }

    public static class toggle_rect implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.setSelectionRectangular(
                    !textArea.isSelectionRectangular());
        }
    }

    public static class insert_char implements ActionListener,
            InputHandler.NonRepeatable {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            final String str = evt.getActionCommand();
            final int repeatCount = textArea.getInputHandler().getRepeatCount();

            if (textArea.isEditable()) {
                textArea.overwriteSetSelectedText(String.valueOf(str).repeat(Math.max(0, repeatCount)));
            } else {
                textArea.getToolkit().beep();
            }
        }
    }

    public static class clip_copy implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.copy();
        }
    }

    public static class clip_paste implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.paste();
        }
    }

    public static class clip_cut implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            final JEditTextArea textArea = InputHandler.getTextArea(evt);
            textArea.cut();
        }
    }
}
