/*
 * JEditTextArea.java - jEdit's text component
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import rars.Globals;
import rars.Settings;
import rars.venus.editors.jeditsyntax.tokenmarker.Token;
import rars.venus.editors.jeditsyntax.tokenmarker.TokenMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * jEdit's text area component. It is more suited for editing program
 * source code than JEditorPane, because it drops the unnecessary features
 * (images, variable-width lines, and so on) and adds a whole bunch of
 * useful goodies such as:
 * <ul>
 * <li>More flexible key binding scheme
 * <li>Supports macro recorders
 * <li>Rectangular selection
 * <li>Bracket highlighting
 * <li>Syntax highlighting
 * <li>Command repetition
 * <li>Block caret can be enabled
 * </ul>
 * It is also faster and doesn't have as many problems. It can be used
 * in other applications; the only other part of jEdit it depends on is
 * the syntax package.
 * <p>
 * <p>
 * To use it in your app, treat it like any other component, for example:
 *
 * <pre>
 * JEditTextArea ta = new JEditTextArea();
 * ta.setTokenMarker(new JavaTokenMarker());
 * ta.setText("public class Test {\n"
 *         + "    public static void main(String[] args) {\n"
 *         + "        System.out.println(\"Hello World\");\n"
 *         + "    }\n"
 *         + "}");
 * </pre>
 *
 * @author Slava Pestov
 * @version $Id: JEditTextArea.java,v 1.36 1999/12/13 03:40:30 sp Exp $
 */
public class JEditTextArea extends JComponent {
    private static final Logger LOGGER = LogManager.getLogger(JEditTextArea.class);
    /**
     * Adding components with this name to the text area will place
     * them left of the horizontal scroll bar. In jEdit, the status
     * bar is added this way.
     */
    public static final String LEFT_OF_SCROLLBAR = "los";
    /**
     * Constant <code>POPUP_HELP_TEXT_COLOR</code>
     */
    public static Color POPUP_HELP_TEXT_COLOR = Color.BLACK; // DPS 11-July-2014

    // Number of text lines moved for each click of the vertical scrollbar buttons.
    private static final int VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES = 1;
    // Number of text lines moved for each "notch" of the mouse wheel scroller.
    private static final int LINES_PER_MOUSE_WHEEL_NOTCH = 3;

    /**
     * Creates a new JEditTextArea with the default settings.
     *
     * @param lineNumbers a {@link javax.swing.JComponent} object
     */
    public JEditTextArea(final JComponent lineNumbers) {
        this(TextAreaDefaults.getDefaults(), lineNumbers);
    }

    private final JScrollBar lineNumbersVertical;

    /**
     * Creates a new JEditTextArea with the specified settings.
     *
     * @param defaults    The default settings
     * @param lineNumbers a {@link javax.swing.JComponent} object
     */
    public JEditTextArea(final TextAreaDefaults defaults, final JComponent lineNumbers) {
        // Enable the necessary events
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);

        // Initialize some misc. stuff
        this.painter = new TextAreaPainter(this, defaults);
        this.documentHandler = new DocumentHandler();
        this.listenerList = new EventListenerList();
        this.caretEvent = new MutableCaretEvent();
        this.lineSegment = new Segment();
        this.bracketLine = this.bracketPosition = -1;
        this.blink = true;
        this.unredoing = false;

        final JScrollPane lineNumberScroller = new JScrollPane(lineNumbers,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        lineNumberScroller.setBorder(new javax.swing.border.EmptyBorder(1, 1, 1, 1));
        this.lineNumbersVertical = lineNumberScroller.getVerticalScrollBar();

        // Initialize the GUI
        final JPanel lineNumbersPlusPainter = new JPanel(new BorderLayout());
        lineNumbersPlusPainter.add(this.painter, BorderLayout.CENTER);
        lineNumbersPlusPainter.add(lineNumberScroller, BorderLayout.WEST);
        this.setLayout(new ScrollLayout());
        this.add(JEditTextArea.CENTER, lineNumbersPlusPainter); // was: painter
        this.add(JEditTextArea.RIGHT, this.vertical = new JScrollBar(JScrollBar.VERTICAL));
        this.add(JEditTextArea.BOTTOM, this.horizontal = new JScrollBar(JScrollBar.HORIZONTAL));

        // Add some event listeners
        this.vertical.addAdjustmentListener(new AdjustHandler());
        this.horizontal.addAdjustmentListener(new AdjustHandler());
        this.painter.addComponentListener(new ComponentHandler());
        this.painter.addMouseListener(new MouseHandler());
        this.painter.addMouseMotionListener(new DragHandler());
        this.painter.addMouseWheelListener(new MouseWheelHandler()); // DPS 5-5-10
        this.addFocusListener(new FocusHandler());

        // Load the defaults
        this.setInputHandler(defaults.inputHandler);
        this.setDocument(defaults.document);
        this.editable = defaults.editable;
        this.caretVisible = defaults.caretVisible;
        this.caretBlinks = defaults.caretBlinks;
        this.caretBlinkRate = defaults.caretBlinkRate;
        this.electricScroll = defaults.electricScroll;

        this.popup = defaults.popup;

        JEditTextArea.caretTimer.setDelay(this.caretBlinkRate);

        // Intercept keystrokes before focus manager gets them. If in editing window,
        // pass (SHIFT) TAB keystrokes on to the key processor instead of letting focus
        // manager use them for focus traversal.
        // One can also accomplish this using: setFocusTraversalKeysEnabled(false);
        // but that seems heavy-handed.
        // DPS 12May2010
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
                e -> {
                    final int modifiers = e.getModifiersEx();
                    if (JEditTextArea.this.isFocusOwner() && e.getKeyCode() == KeyEvent.VK_TAB
                            && (modifiers == 0 || (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)) {
                        JEditTextArea.this.processKeyEvent(e);
                        return true;
                    } else {
                        return false;
                    }
                });

        // We don't seem to get the initial focus event?
        JEditTextArea.focusedComponent = this;
    }

    /*
     * Returns if this component can be traversed by pressing
     * the Tab key. This returns false.
     */
    // public final boolean isManagingFocus()
    // {
    // return true;
    // }

    /**
     * Returns the object responsible for painting this text area.
     *
     * @return a {@link TextAreaPainter} object
     */
    public final TextAreaPainter getPainter() {
        return this.painter;
    }

    /**
     * Returns the input handler.
     *
     * @return a {@link InputHandler} object
     */
    public final InputHandler getInputHandler() {
        return this.inputHandler;
    }

    /**
     * Sets the input handler.
     *
     * @param inputHandler The new input handler
     */
    public void setInputHandler(final InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    /**
     * Returns true if the caret is blinking, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isCaretBlinkEnabled() {
        return this.caretBlinks;
    }

    /**
     * Toggles caret blinking.
     *
     * @param caretBlinks True if the caret should blink, false otherwise
     */
    public void setCaretBlinkEnabled(final boolean caretBlinks) {
        this.caretBlinks = caretBlinks;
        if (!caretBlinks)
            this.blink = false;

        this.painter.invalidateSelectedLines();
    }

    /**
     * Returns true if the caret is visible, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isCaretVisible() {
        return (!this.caretBlinks || this.blink) && this.caretVisible;
    }

    /**
     * Sets if the caret should be visible.
     *
     * @param caretVisible True if the caret should be visible, false
     *                     otherwise
     */
    public void setCaretVisible(final boolean caretVisible) {
        this.caretVisible = caretVisible;
        this.blink = true;

        this.painter.invalidateSelectedLines();
    }

    /**
     * Blinks the caret.
     */
    public final void blinkCaret() {
        if (this.caretBlinks) {
            this.blink = !this.blink;
            this.painter.invalidateSelectedLines();
        } else
            this.blink = true;
    }

    /**
     * Returns the number of lines from the top and button of the
     * text area that are always visible.
     *
     * @return a int
     */
    public final int getElectricScroll() {
        return this.electricScroll;
    }

    /**
     * Sets the number of lines from the top and bottom of the text
     * area that are always visible
     *
     * @param electricScroll The number of lines always visible from
     *                       the top or bottom
     */
    public final void setElectricScroll(final int electricScroll) {
        this.electricScroll = electricScroll;
    }

    /**
     * Updates the state of the scroll bars. This should be called
     * if the number of lines in the document changes, or when the
     * size of the text are changes.
     */
    public void updateScrollBars() {
        if (this.vertical != null && this.visibleLines != 0) {
            this.vertical.setValues(this.firstLine, this.visibleLines, 0, this.getLineCount());
            this.vertical.setUnitIncrement(JEditTextArea.VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES);
            this.vertical.setBlockIncrement(this.visibleLines);

            // Editing area scrollbar has custom model that increments by number of text
            // lines instead of
            // number of pixels. The line number display uses a standard (but invisible)
            // scrollbar based
            // on pixels, so I need to adjust accordingly to keep it in synch with the
            // editing area scrollbar.
            // DPS 4-May-2010
            final int height = this.painter.getFontMetrics(this.painter.getFont()).getHeight();
            this.lineNumbersVertical.setValues(this.firstLine * height, this.visibleLines * height, 0, this.getLineCount() * height);
            this.lineNumbersVertical.setUnitIncrement(JEditTextArea.VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES * height);
            this.lineNumbersVertical.setBlockIncrement(this.visibleLines * height);
        }

        final int width = this.painter.getWidth();
        if (this.horizontal != null && width != 0) {
            this.horizontal.setValues(-this.horizontalOffset, width, 0, width * 5);
            this.horizontal.setUnitIncrement(this.painter.getFontMetrics()
                    .charWidth('w'));
            this.horizontal.setBlockIncrement(width / 2);
        }
    }

    /**
     * Returns the line displayed at the text area's origin.
     *
     * @return a int
     */
    public final int getFirstLine() {
        return this.firstLine;
    }

    /**
     * Sets the line displayed at the text area's origin and
     * updates the scroll bars.
     *
     * @param firstLine a int
     */
    public void setFirstLine(final int firstLine) {
        if (firstLine == this.firstLine)
            return;
        final int oldFirstLine = this.firstLine;
        this.firstLine = firstLine;
        this.updateScrollBars();
        this.painter.repaint();
    }

    /**
     * Returns the number of lines visible in this text area.
     *
     * @return a int
     */
    public final int getVisibleLines() {
        return this.visibleLines;
    }

    /**
     * Recalculates the number of visible lines. This should not
     * be called directly.
     */
    public final void recalculateVisibleLines() {
        if (this.painter == null)
            return;
        final int height = this.painter.getHeight();
        final int lineHeight = this.painter.getFontMetrics().getHeight();
        final int oldVisibleLines = this.visibleLines;
        this.visibleLines = height / lineHeight;
        this.updateScrollBars();
    }

    /**
     * Returns the horizontal offset of drawn lines.
     *
     * @return a int
     */
    public final int getHorizontalOffset() {
        return this.horizontalOffset;
    }

    /**
     * Sets the horizontal offset of drawn lines. This can be used to
     * implement horizontal scrolling.
     *
     * @param horizontalOffset offset The new horizontal offset
     */
    public void setHorizontalOffset(final int horizontalOffset) {
        if (horizontalOffset == this.horizontalOffset)
            return;
        this.horizontalOffset = horizontalOffset;
        if (horizontalOffset != this.horizontal.getValue())
            this.updateScrollBars();
        this.painter.repaint();
    }

    /**
     * A fast way of changing both the first line and horizontal
     * offset.
     *
     * @param firstLine        The new first line
     * @param horizontalOffset The new horizontal offset
     * @return True if any of the values were changed, false otherwise
     */
    public boolean setOrigin(final int firstLine, final int horizontalOffset) {
        boolean changed = false;
        final int oldFirstLine = this.firstLine;

        if (horizontalOffset != this.horizontalOffset) {
            this.horizontalOffset = horizontalOffset;
            changed = true;
        }

        if (firstLine != this.firstLine) {
            this.firstLine = firstLine;
            changed = true;
        }

        if (changed) {
            this.updateScrollBars();
            this.painter.repaint();
        }

        return changed;
    }

    /**
     * Ensures that the caret is visible by scrolling the text area if
     * necessary.
     *
     * @return True if scrolling was actually performed, false if the
     * caret was already visible
     */
    public boolean scrollToCaret() {
        final int line = this.getCaretLine();
        final int lineStart = this.getLineStartOffset(line);
        final int offset = Math.max(0, Math.min(this.getLineLength(line) - 1,
                this.getCaretPosition() - lineStart));

        return this.scrollTo(line, offset);
    }

    /**
     * Ensures that the specified line and offset is visible by scrolling
     * the text area if necessary.
     *
     * @param line   The line to scroll to
     * @param offset The offset in the line to scroll to
     * @return True if scrolling was actually performed, false if the
     * line and offset was already visible
     */
    public boolean scrollTo(final int line, final int offset) {
        // visibleLines == 0 before the component is realized
        // we can't do any proper scrolling then, so we have
        // this hack...
        if (this.visibleLines == 0) {
            this.setFirstLine(Math.max(0, line - this.electricScroll));
            return true;
        }

        int newFirstLine = this.firstLine;
        int newHorizontalOffset = this.horizontalOffset;

        if (line < this.firstLine + this.electricScroll) {
            newFirstLine = Math.max(0, line - this.electricScroll);
        } else if (line + this.electricScroll >= this.firstLine + this.visibleLines) {
            newFirstLine = (line - this.visibleLines) + this.electricScroll + 1;
            if (newFirstLine + this.visibleLines >= this.getLineCount())
                newFirstLine = this.getLineCount() - this.visibleLines;
            if (newFirstLine < 0)
                newFirstLine = 0;
        }

        final int x = this._offsetToX(line, offset);
        final int width = this.painter.getFontMetrics().charWidth('w');

        if (x < 0) {
            newHorizontalOffset = Math.min(0, this.horizontalOffset
                    - x + width + 5);
        } else if (x + width >= this.painter.getWidth()) {
            newHorizontalOffset = this.horizontalOffset +
                    (this.painter.getWidth() - x) - width - 5;
        }

        return this.setOrigin(newFirstLine, newHorizontalOffset);
    }

    /**
     * Converts a line index to a y co-ordinate.
     *
     * @param line The line
     * @return a int
     */
    public int lineToY(final int line) {
        final FontMetrics fm = this.painter.getFontMetrics();
        return (line - this.firstLine) * fm.getHeight()
                - (fm.getLeading() + fm.getMaxDescent());
    }

    /**
     * Converts a y co-ordinate to a line index.
     *
     * @param y The y co-ordinate
     * @return a int
     */
    public int yToLine(final int y) {
        final FontMetrics fm = this.painter.getFontMetrics();
        final int height = fm.getHeight();
        return Math.max(0, Math.min(this.getLineCount() - 1,
                y / height + this.firstLine));
    }

    /**
     * Converts an offset in a line into an x co-ordinate. This is a
     * slow version that can be used any time.
     *
     * @param line   The line
     * @param offset The offset, from the start of the line
     * @return a int
     */
    public final int offsetToX(final int line, final int offset) {
        // don't use cached tokens
        this.painter.currentLineTokens = null;
        return this._offsetToX(line, offset);
    }

    /**
     * Converts an offset in a line into an x co-ordinate. This is a
     * fast version that should only be used if no changes were made
     * to the text since the last repaint.
     *
     * @param line   The line
     * @param offset The offset, from the start of the line
     * @return a int
     */
    public int _offsetToX(final int line, final int offset) {
        final TokenMarker tokenMarker = this.getTokenMarker();

        /* Use painter's cached info for speed */
        FontMetrics fm = this.painter.getFontMetrics();

        this.getLineText(line, this.lineSegment);

        final int segmentOffset = this.lineSegment.offset;
        int x = this.horizontalOffset;

        /* If syntax coloring is disabled, do simple translation */
        if (tokenMarker == null) {
            this.lineSegment.count = offset;
            return x + (int) Utilities.getTabbedTextWidth(this.lineSegment,
                    fm, (float) x, this.painter, 0);
        }
        /*
         * If syntax coloring is enabled, we have to do this because
         * tokens can vary in width
         */
        else {
            Token tokens;
            if (this.painter.currentLineIndex == line
                    && this.painter.currentLineTokens != null)
                tokens = this.painter.currentLineTokens;
            else {
                this.painter.currentLineIndex = line;
                tokens = this.painter.currentLineTokens = tokenMarker.markTokens(this.lineSegment, line);
            }

            final Toolkit toolkit = this.painter.getToolkit();
            final Font defaultFont = this.painter.getFont();
            final SyntaxStyle[] styles = this.painter.getStyles();

            for (; ; ) {
                final byte id = tokens.id;
                if (id == Token.END) {
                    return x;
                }

                if (id == Token.NULL)
                    fm = this.painter.getFontMetrics();
                else
                    fm = styles[id].getFontMetrics(defaultFont, this.getGraphics());

                final int length = tokens.length;

                if (offset + segmentOffset < this.lineSegment.offset + length) {
                    this.lineSegment.count = offset - (this.lineSegment.offset - segmentOffset);
                    return x + (int) Utilities.getTabbedTextWidth(
                            this.lineSegment, fm, (float) x, this.painter, 0);
                } else {
                    this.lineSegment.count = length;
                    x += (int) Utilities.getTabbedTextWidth(
                            this.lineSegment, fm, (float) x, this.painter, 0);
                    this.lineSegment.offset += length;
                }
                tokens = tokens.next;
            }
        }
    }

    /**
     * Converts an x co-ordinate to an offset within a line.
     *
     * @param line The line
     * @param x    The x co-ordinate
     * @return a int
     */
    public int xToOffset(final int line, final int x) {
        final TokenMarker tokenMarker = this.getTokenMarker();

        /* Use painter's cached info for speed */
        FontMetrics fm = this.painter.getFontMetrics();

        this.getLineText(line, this.lineSegment);

        final char[] segmentArray = this.lineSegment.array;
        final int segmentOffset = this.lineSegment.offset;
        final int segmentCount = this.lineSegment.count;

        int width = this.horizontalOffset;

        if (tokenMarker == null) {
            for (int i = 0; i < segmentCount; i++) {
                final char c = segmentArray[i + segmentOffset];
                final int charWidth;
                if (c == '\t')
                    charWidth = (int) this.painter.nextTabStop(width, i)
                            - width;
                else
                    charWidth = fm.charWidth(c);

                if (this.painter.isBlockCaretEnabled()) {
                    if (x - charWidth <= width)
                        return i;
                } else {
                    if (x - charWidth / 2 <= width)
                        return i;
                }

                width += charWidth;
            }

            return segmentCount;
        } else {
            Token tokens;
            if (this.painter.currentLineIndex == line && this.painter.currentLineTokens != null)
                tokens = this.painter.currentLineTokens;
            else {
                this.painter.currentLineIndex = line;
                tokens = this.painter.currentLineTokens = tokenMarker.markTokens(this.lineSegment, line);
            }

            int offset = 0;
            final Toolkit toolkit = this.painter.getToolkit();
            final Font defaultFont = this.painter.getFont();
            final SyntaxStyle[] styles = this.painter.getStyles();

            for (; ; ) {
                final byte id = tokens.id;
                if (id == Token.END)
                    return offset;

                if (id == Token.NULL)
                    fm = this.painter.getFontMetrics();
                else
                    fm = styles[id].getFontMetrics(defaultFont, this.getGraphics());

                final int length = tokens.length;

                for (int i = 0; i < length; i++) {
                    final char c = segmentArray[segmentOffset + offset + i];
                    final int charWidth;
                    if (c == '\t')
                        charWidth = (int) this.painter.nextTabStop(width, offset + i)
                                - width;
                    else
                        charWidth = fm.charWidth(c);

                    if (this.painter.isBlockCaretEnabled()) {
                        if (x - charWidth <= width)
                            return offset + i;
                    } else {
                        if (x - charWidth / 2 <= width)
                            return offset + i;
                    }

                    width += charWidth;
                }

                offset += length;
                tokens = tokens.next;
            }
        }
    }

    /**
     * Converts a point to an offset, from the start of the text.
     *
     * @param x The x co-ordinate of the point
     * @param y The y co-ordinate of the point
     * @return a int
     */
    public int xyToOffset(final int x, final int y) {
        final int line = this.yToLine(y);
        final int start = this.getLineStartOffset(line);
        return start + this.xToOffset(line, x);
    }

    /**
     * Returns the document this text area is editing.
     *
     * @return a {@link javax.swing.text.Document} object
     */
    public final Document getDocument() {
        return this.document;
    }

    /**
     * Sets the document this text area is editing.
     *
     * @param document The document
     */
    public void setDocument(final SyntaxDocument document) {
        if (this.document == document)
            return;
        if (this.document != null)
            this.document.removeDocumentListener(this.documentHandler);
        this.document = document;

        document.addDocumentListener(this.documentHandler);

        this.select(0, 0);
        this.updateScrollBars();
        this.painter.repaint();
    }

    /**
     * Returns the document's token marker. Equivalent to calling
     * <code>getDocument().getTokenMarker()</code>.
     *
     * @return a {@link TokenMarker} object
     */
    public final TokenMarker getTokenMarker() {
        return this.document.getTokenMarker();
    }

    /**
     * Sets the document's token marker. Equivalent to caling
     * <code>getDocument().setTokenMarker()</code>.
     *
     * @param tokenMarker The token marker
     */
    public final void setTokenMarker(final TokenMarker tokenMarker) {
        this.document.setTokenMarker(tokenMarker);
    }

    /**
     * Returns the length of the document. Equivalent to calling
     * <code>getDocument().getLength()</code>.
     *
     * @return a int
     */
    public final int getDocumentLength() {
        return this.document.getLength();
    }

    /**
     * Returns the number of lines in the document.
     *
     * @return a int
     */
    public final int getLineCount() {
        return this.document.getDefaultRootElement().getElementCount();
    }

    /**
     * Returns the line containing the specified offset.
     *
     * @param offset The offset
     * @return a int
     */
    public final int getLineOfOffset(final int offset) {
        return this.document.getDefaultRootElement().getElementIndex(offset);
    }

    /**
     * Returns the start offset of the specified line.
     *
     * @param line The line
     * @return The start offset of the specified line, or -1 if the line is
     * invalid
     */
    public int getLineStartOffset(final int line) {
        final Element lineElement = this.document.getDefaultRootElement()
                .getElement(line);
        if (lineElement == null)
            return -1;
        else
            return lineElement.getStartOffset();
    }

    /**
     * Returns the end offset of the specified line.
     *
     * @param line The line
     * @return The end offset of the specified line, or -1 if the line is
     * invalid.
     */
    public int getLineEndOffset(final int line) {
        final Element lineElement = this.document.getDefaultRootElement()
                .getElement(line);
        if (lineElement == null)
            return -1;
        else
            return lineElement.getEndOffset();
    }

    /**
     * Returns the length of the specified line.
     *
     * @param line The line
     * @return a int
     */
    public int getLineLength(final int line) {
        final Element lineElement = this.document.getDefaultRootElement()
                .getElement(line);
        if (lineElement == null)
            return -1;
        else
            return lineElement.getEndOffset()
                    - lineElement.getStartOffset() - 1;
    }

    /**
     * Returns the entire text of this text area.
     *
     * @return a {@link java.lang.String} object
     */
    public String getText() {
        try {
            return this.document.getText(0, this.document.getLength());
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error getting text from document", bl);
            return null;
        }
    }

    /**
     * Sets the entire text of this text area.
     *
     * @param text a {@link java.lang.String} object
     */
    public void setText(final String text) {
        try {
            this.document.beginCompoundEdit();
            this.document.remove(0, this.document.getLength());
            this.document.insertString(0, text, null);
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error setting text in document", bl);
        } finally {
            this.document.endCompoundEdit();
        }
    }

    /**
     * Returns the specified substring of the document.
     *
     * @param start The start offset
     * @param len   The length of the substring
     * @return The substring, or null if the offsets are invalid
     */
    public final String getText(final int start, final int len) {
        try {
            return this.document.getText(start, len);
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error getting text from document", bl);
            return null;
        }
    }

    /**
     * Copies the specified substring of the document into a segment.
     * If the offsets are invalid, the segment will contain a null string.
     *
     * @param start   The start offset
     * @param len     The length of the substring
     * @param segment The segment
     */
    public final void getText(final int start, final int len, final Segment segment) {
        try {
            this.document.getText(start, len, segment);
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error getting text from document", bl);
            segment.offset = segment.count = 0;
        }
    }

    /**
     * Returns the text on the specified line.
     *
     * @param lineIndex The line
     * @return The text, or null if the line is invalid
     */
    public final String getLineText(final int lineIndex) {
        final int start = this.getLineStartOffset(lineIndex);
        return this.getText(start, this.getLineEndOffset(lineIndex) - start - 1);
    }

    /**
     * Copies the text on the specified line into a segment. If the line
     * is invalid, the segment will contain a null string.
     *
     * @param lineIndex The line
     * @param segment   a {@link javax.swing.text.Segment} object
     */
    public final void getLineText(final int lineIndex, final Segment segment) {
        final int start = this.getLineStartOffset(lineIndex);
        this.getText(start, this.getLineEndOffset(lineIndex) - start - 1, segment);
    }

    /**
     * Returns the selection start offset.
     *
     * @return a int
     */
    public final int getSelectionStart() {
        return this.selectionStart;
    }

    /**
     * Returns the offset where the selection starts on the specified
     * line.
     *
     * @param line a int
     * @return a int
     */
    public int getSelectionStart(final int line) {
        if (line == this.selectionStartLine)
            return this.selectionStart;
        else if (this.rectSelect) {
            final Element map = this.document.getDefaultRootElement();
            final int start = this.selectionStart - map.getElement(this.selectionStartLine)
                    .getStartOffset();

            final Element lineElement = map.getElement(line);
            final int lineStart = lineElement.getStartOffset();
            final int lineEnd = lineElement.getEndOffset() - 1;
            return Math.min(lineEnd, lineStart + start);
        } else
            return this.getLineStartOffset(line);
    }

    /**
     * Returns the selection start line.
     *
     * @return a int
     */
    public final int getSelectionStartLine() {
        return this.selectionStartLine;
    }

    /**
     * Sets the selection start. The new selection will be the new
     * selection start and the old selection end.
     *
     * @param selectionStart The selection start
     * @see #select(int, int)
     */
    public final void setSelectionStart(final int selectionStart) {
        this.select(selectionStart, this.selectionEnd);
    }

    /**
     * Returns the selection end offset.
     *
     * @return a int
     */
    public final int getSelectionEnd() {
        return this.selectionEnd;
    }

    /**
     * Returns the offset where the selection ends on the specified
     * line.
     *
     * @param line a int
     * @return a int
     */
    public int getSelectionEnd(final int line) {
        if (line == this.selectionEndLine)
            return this.selectionEnd;
        else if (this.rectSelect) {
            final Element map = this.document.getDefaultRootElement();
            final int end = this.selectionEnd - map.getElement(this.selectionEndLine)
                    .getStartOffset();

            final Element lineElement = map.getElement(line);
            final int lineStart = lineElement.getStartOffset();
            final int lineEnd = lineElement.getEndOffset() - 1;
            return Math.min(lineEnd, lineStart + end);
        } else
            return this.getLineEndOffset(line) - 1;
    }

    /**
     * Returns the selection end line.
     *
     * @return a int
     */
    public final int getSelectionEndLine() {
        return this.selectionEndLine;
    }

    /**
     * Sets the selection end. The new selection will be the old
     * selection start and the new selection end.
     *
     * @param selectionEnd The selection end
     * @see #select(int, int)
     */
    public final void setSelectionEnd(final int selectionEnd) {
        this.select(this.selectionStart, selectionEnd);
    }

    /**
     * Returns the caret position. This will either be the selection
     * start or the selection end, depending on which direction the
     * selection was made in.
     *
     * @return a int
     */
    public final int getCaretPosition() {
        return (this.biasLeft ? this.selectionStart : this.selectionEnd);
    }

    /**
     * Returns the caret line.
     *
     * @return a int
     */
    public final int getCaretLine() {
        return (this.biasLeft ? this.selectionStartLine : this.selectionEndLine);
    }

    /**
     * Returns the mark position. This will be the opposite selection
     * bound to the caret position.
     *
     * @return a int
     * @see #getCaretPosition()
     */
    public final int getMarkPosition() {
        return (this.biasLeft ? this.selectionEnd : this.selectionStart);
    }

    /**
     * Returns the mark line.
     *
     * @return a int
     */
    public final int getMarkLine() {
        return (this.biasLeft ? this.selectionEndLine : this.selectionStartLine);
    }

    /**
     * Sets the caret position. The new selection will consist of the
     * caret position only (hence no text will be selected)
     *
     * @param caret The caret position
     * @see #select(int, int)
     */
    public final void setCaretPosition(final int caret) {
        this.select(caret, caret);
    }

    /**
     * Selects all text in the document.
     */
    public final void selectAll() {
        this.select(0, this.getDocumentLength());
    }

    /**
     * Moves the mark to the caret position.
     */
    public final void selectNone() {
        this.select(this.getCaretPosition(), this.getCaretPosition());
    }

    /**
     * Selects from the start offset to the end offset. This is the
     * general selection method used by all other selecting methods.
     * The caret position will be start if start &lt; end, and end
     * if end &gt; start.
     *
     * @param start The start offset
     * @param end   The end offset
     */
    public void select(final int start, final int end) {
        final int newStart;
        final int newEnd;
        final boolean newBias;
        if (start <= end) {
            newStart = start;
            newEnd = end;
            newBias = false;
        } else {
            newStart = end;
            newEnd = start;
            newBias = true;
        }

        if (newStart < 0 || newEnd > this.getDocumentLength()) {
            throw new IllegalArgumentException("Bounds out of"
                    + " range: " + newStart + "," +
                    newEnd);
        }

        // If the new position is the same as the old, we don't
        // do all this crap, however we still do the stuff at
        // the end (clearing magic position, scrolling)
        if (newStart != this.selectionStart || newEnd != this.selectionEnd
                || newBias != this.biasLeft) {
            final int newStartLine = this.getLineOfOffset(newStart);
            final int newEndLine = this.getLineOfOffset(newEnd);

            if (this.painter.isBracketHighlightEnabled()) {
                if (this.bracketLine != -1)
                    this.painter.invalidateLine(this.bracketLine);
                this.updateBracketHighlight(end);
                if (this.bracketLine != -1)
                    this.painter.invalidateLine(this.bracketLine);
            }

            this.painter.invalidateLineRange(this.selectionStartLine, this.selectionEndLine);
            this.painter.invalidateLineRange(newStartLine, newEndLine);

            this.document.addUndoableEdit(new CaretUndo(
                    this.selectionStart, this.selectionEnd));

            this.selectionStart = newStart;
            this.selectionEnd = newEnd;
            this.selectionStartLine = newStartLine;
            this.selectionEndLine = newEndLine;
            this.biasLeft = newBias;

            this.fireCaretEvent();
        }

        // When the user is typing, etc, we don't want the caret
        // to blink
        this.blink = true;
        JEditTextArea.caretTimer.restart();

        // Disable rectangle select if selection start = selection end
        if (this.selectionStart == this.selectionEnd)
            this.rectSelect = false;

        // Clear the `magic' caret position used by up/down
        this.magicCaret = -1;
        this.scrollToCaret();
    }

    /**
     * Returns the selected text, or null if no selection is active.
     *
     * @return a {@link java.lang.String} object
     */
    public final String getSelectedText() {
        if (this.selectionStart == this.selectionEnd)
            return null;

        if (this.rectSelect) {
            // Return each row of the selection on a new line

            final Element map = this.document.getDefaultRootElement();

            int start = this.selectionStart - map.getElement(this.selectionStartLine)
                    .getStartOffset();
            int end = this.selectionEnd - map.getElement(this.selectionEndLine)
                    .getStartOffset();

            // Certain rectangles satisfy this condition...
            if (end < start) {
                final int tmp = end;
                end = start;
                start = tmp;
            }

            final StringBuilder buf = new StringBuilder();
            final Segment seg = new Segment();

            for (int i = this.selectionStartLine; i <= this.selectionEndLine; i++) {
                final Element lineElement = map.getElement(i);
                int lineStart = lineElement.getStartOffset();
                final int lineEnd = lineElement.getEndOffset() - 1;
                final int lineLen;

                lineStart = Math.min(lineStart + start, lineEnd);
                lineLen = Math.min(end - start, lineEnd - lineStart);

                this.getText(lineStart, lineLen, seg);
                buf.append(seg.array, seg.offset, seg.count);

                if (i != this.selectionEndLine)
                    buf.append('\n');
            }

            return buf.toString();
        } else {
            return this.getText(this.selectionStart,
                    this.selectionEnd - this.selectionStart);
        }
    }

    /**
     * Replaces the selection with the specified text.
     *
     * @param selectedText The replacement text for the selection
     */
    public void setSelectedText(final String selectedText) {
        if (!this.editable) {
            throw new InternalError("Text component"
                    + " read only");
        }

        this.document.beginCompoundEdit();

        try {
            if (this.rectSelect) {
                final Element map = this.document.getDefaultRootElement();

                int start = this.selectionStart - map.getElement(this.selectionStartLine)
                        .getStartOffset();
                int end = this.selectionEnd - map.getElement(this.selectionEndLine)
                        .getStartOffset();

                // Certain rectangles satisfy this condition...
                if (end < start) {
                    final int tmp = end;
                    end = start;
                    start = tmp;
                }

                int lastNewline = 0;
                int currNewline = 0;

                for (int i = this.selectionStartLine; i <= this.selectionEndLine; i++) {
                    final Element lineElement = map.getElement(i);
                    final int lineStart = lineElement.getStartOffset();
                    final int lineEnd = lineElement.getEndOffset() - 1;
                    final int rectStart = Math.min(lineEnd, lineStart + start);

                    this.document.remove(rectStart, Math.min(lineEnd - rectStart,
                            end - start));

                    if (selectedText == null)
                        continue;

                    currNewline = selectedText.indexOf('\n', lastNewline);
                    if (currNewline == -1)
                        currNewline = selectedText.length();

                    this.document.insertString(rectStart, selectedText
                            .substring(lastNewline, currNewline), null);

                    lastNewline = Math.min(selectedText.length(),
                            currNewline + 1);
                }

                if (selectedText != null &&
                        currNewline != selectedText.length()) {
                    final int offset = map.getElement(this.selectionEndLine)
                            .getEndOffset() - 1;
                    this.document.insertString(offset, "\n", null);
                    this.document.insertString(offset + 1, selectedText
                            .substring(currNewline + 1), null);
                }
            } else {
                this.document.remove(this.selectionStart,
                        this.selectionEnd - this.selectionStart);
                if (selectedText != null) {
                    this.document.insertString(this.selectionStart,
                            selectedText, null);
                }
            }
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error setting selected text", bl);
            throw new InternalError("Cannot replace"
                    + " selection");
        }
        // No matter what happends... stops us from leaving document
        // in a bad state
        finally {
            this.document.endCompoundEdit();
        }

        this.setCaretPosition(this.selectionEnd);
    }

    /**
     * Returns true if this text area is editable, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isEditable() {
        return this.editable;
    }

    /**
     * Sets if this component is editable.
     *
     * @param editable True if this text area should be editable,
     *                 false otherwise
     */
    public final void setEditable(final boolean editable) {
        this.editable = editable;
    }

    /**
     * Returns the right click popup menu.
     *
     * @return a {@link javax.swing.JPopupMenu} object
     */
    public final JPopupMenu getRightClickPopup() {
        return this.popup;
    }

    /**
     * Sets the right click popup menu.
     *
     * @param popup The popup
     */
    public final void setRightClickPopup(final JPopupMenu popup) {
        this.popup = popup;
    }

    /**
     * Returns the `magic' caret position. This can be used to preserve
     * the column position when moving up and down lines.
     *
     * @return a int
     */
    public final int getMagicCaretPosition() {
        return this.magicCaret;
    }

    /**
     * Sets the `magic' caret position. This can be used to preserve
     * the column position when moving up and down lines.
     *
     * @param magicCaret The magic caret position
     */
    public final void setMagicCaretPosition(final int magicCaret) {
        this.magicCaret = magicCaret;
    }

    /**
     * Similar to <code>setSelectedText()</code>, but overstrikes the
     * appropriate number of characters if overwrite mode is enabled.
     *
     * @param str The string
     * @see #setSelectedText(String)
     * @see #isOverwriteEnabled()
     */
    public void overwriteSetSelectedText(final String str) {
        // Don't overstrike if there is a selection
        if (!this.overwrite || this.selectionStart != this.selectionEnd) {
            this.setSelectedText(str);
            this.applySyntaxSensitiveHelp();
            return;
        }

        // Don't overstrike if we're on the end of
        // the line
        final int caret = this.getCaretPosition();
        final int caretLineEnd = this.getLineEndOffset(this.getCaretLine());
        if (caretLineEnd - caret <= str.length()) {
            this.setSelectedText(str);
            this.applySyntaxSensitiveHelp();
            return;
        }

        this.document.beginCompoundEdit();

        try {
            this.document.remove(caret, str.length());
            this.document.insertString(caret, str, null);
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error overwriting text", bl);
        } finally {
            this.document.endCompoundEdit();
        }
        this.applySyntaxSensitiveHelp();

    }

    JPopupMenu popupMenu;

    /**
     * Returns true if overwrite mode is enabled, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isOverwriteEnabled() {
        return this.overwrite;
    }

    /**
     * Sets if overwrite mode should be enabled.
     *
     * @param overwrite True if overwrite mode should be enabled,
     *                  false otherwise.
     */
    public final void setOverwriteEnabled(final boolean overwrite) {
        this.overwrite = overwrite;
        this.painter.invalidateSelectedLines();
    }

    /**
     * Returns true if the selection is rectangular, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isSelectionRectangular() {
        return this.rectSelect;
    }

    /**
     * Sets if the selection should be rectangular.
     *
     * @param rectSelect True if the selection should be rectangular,
     *                   false otherwise.
     */
    public final void setSelectionRectangular(final boolean rectSelect) {
        this.rectSelect = rectSelect;
        this.painter.invalidateSelectedLines();
    }

    /**
     * Returns the position of the highlighted bracket (the bracket
     * matching the one before the caret)
     *
     * @return a int
     */
    public final int getBracketPosition() {
        return this.bracketPosition;
    }

    /**
     * Returns the line of the highlighted bracket (the bracket
     * matching the one before the caret)
     *
     * @return a int
     */
    public final int getBracketLine() {
        return this.bracketLine;
    }

    /**
     * Adds a caret change listener to this text area.
     *
     * @param listener The listener
     */
    public final void addCaretListener(final CaretListener listener) {
        this.listenerList.add(CaretListener.class, listener);
    }

    /**
     * Removes a caret change listener from this text area.
     *
     * @param listener The listener
     */
    public final void removeCaretListener(final CaretListener listener) {
        this.listenerList.remove(CaretListener.class, listener);
    }

    /**
     * Deletes the selected text from the text area and places it
     * into the clipboard.
     */
    public void cut() {
        if (this.editable) {
            this.copy();
            this.setSelectedText("");
        }
    }

    /**
     * Places the selected text into the clipboard.
     */
    public void copy() {
        if (this.selectionStart != this.selectionEnd) {
            final Clipboard clipboard = this.getToolkit().getSystemClipboard();

            final String selection = this.getSelectedText();

            final int repeatCount = this.inputHandler.getRepeatCount();

            clipboard.setContents(new StringSelection(String.valueOf(selection).repeat(Math.max(0, repeatCount))), null);
        }
    }

    /**
     * Inserts the clipboard contents into the text.
     */
    public void paste() {
        if (this.editable) {
            final Clipboard clipboard = this.getToolkit().getSystemClipboard();
            try {
                // The MacOS MRJ doesn't convert \r to \n,
                // so do it here
                String selection = ((String) clipboard
                        .getContents(this).getTransferData(
                                DataFlavor.stringFlavor))
                        .replace('\r', '\n');

                final int repeatCount = this.inputHandler.getRepeatCount();
                selection = selection.repeat(Math.max(0, repeatCount));
                this.setSelectedText(selection);
            } catch (final Exception e) {
                this.getToolkit().beep();
                JEditTextArea.LOGGER.error("Clipboard does not contain a string", e);
            }
        }
    }

    /**
     * Called by the AWT when this component is removed from it's parent.
     * This stops clears the currently focused component.
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (JEditTextArea.focusedComponent == this)
            JEditTextArea.focusedComponent = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Forwards key events directly to the input handler.
     * This is slightly faster than using a KeyListener
     * because some Swing overhead is avoided.
     */
    @Override
    public void processKeyEvent(final KeyEvent evt) {
        if (this.inputHandler == null)
            return;
        switch (evt.getID()) {
            case KeyEvent.KEY_TYPED:
                this.inputHandler.keyTyped(evt);
                break;
            case KeyEvent.KEY_PRESSED:
                if (!this.checkPopupCompletion(evt)) {
                    this.inputHandler.keyPressed(evt);
                }
                this.checkPopupMenu(evt);
                break;
            case KeyEvent.KEY_RELEASED:
                this.inputHandler.keyReleased(evt);
                break;
        }
    }

    // protected members
    /**
     * Constant <code>CENTER="center"</code>
     */
    protected static final String CENTER = "center";
    /**
     * Constant <code>RIGHT="right"</code>
     */
    protected static final String RIGHT = "right";
    /**
     * Constant <code>BOTTOM="bottom"</code>
     */
    protected static final String BOTTOM = "bottom";

    /**
     * Constant <code>focusedComponent</code>
     */
    protected static JEditTextArea focusedComponent;
    /**
     * Constant <code>caretTimer</code>
     */
    protected static final Timer caretTimer;

    protected TextAreaPainter painter;

    protected JPopupMenu popup;

    protected EventListenerList listenerList;
    protected MutableCaretEvent caretEvent;

    protected boolean caretBlinks;
    protected boolean caretVisible;
    protected boolean blink;

    protected boolean editable;

    protected int caretBlinkRate;
    protected int firstLine;
    protected int visibleLines;
    protected int electricScroll;

    protected int horizontalOffset;

    protected JScrollBar vertical;
    protected JScrollBar horizontal;
    protected boolean scrollBarsInitialized;

    protected InputHandler inputHandler;
    protected SyntaxDocument document;
    protected DocumentHandler documentHandler;

    protected Segment lineSegment;

    protected int selectionStart;
    protected int selectionStartLine;
    protected int selectionEnd;
    protected int selectionEndLine;
    protected boolean biasLeft;

    protected int bracketPosition;
    protected int bracketLine;

    protected int magicCaret;
    protected boolean overwrite;
    protected boolean rectSelect;
    // "unredoing" is mode used by DocumentHandler's insertUpdate() and
    // removeUpdate()
    // to pleasingly select the text and location of the undo. DPS 3-May-2010
    protected boolean unredoing;

    /**
     * <p>fireCaretEvent.</p>
     */
    protected void fireCaretEvent() {
        final Object[] listeners = this.listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i--) {
            if (listeners[i] == CaretListener.class) {
                ((CaretListener) listeners[i + 1]).caretUpdate(this.caretEvent);
            }
        }
    }

    /**
     * <p>updateBracketHighlight.</p>
     *
     * @param newCaretPosition a int
     */
    protected void updateBracketHighlight(final int newCaretPosition) {
        if (newCaretPosition == 0) {
            this.bracketPosition = this.bracketLine = -1;
            return;
        }

        try {
            final int offset = TextUtilities.findMatchingBracket(
                    this.document, newCaretPosition - 1);
            if (offset != -1) {
                this.bracketLine = this.getLineOfOffset(offset);
                this.bracketPosition = offset - this.getLineStartOffset(this.bracketLine);
                return;
            }
        } catch (final BadLocationException bl) {
            JEditTextArea.LOGGER.error("Error updating bracket highlight", bl);
        }

        this.bracketLine = this.bracketPosition = -1;
    }

    /**
     * <p>documentChanged.</p>
     *
     * @param evt a {@link javax.swing.event.DocumentEvent} object
     */
    protected void documentChanged(final DocumentEvent evt) {
        final DocumentEvent.ElementChange ch = evt.getChange(
                this.document.getDefaultRootElement());

        final int count;
        if (ch == null)
            count = 0;
        else
            count = ch.getChildrenAdded().length -
                    ch.getChildrenRemoved().length;

        final int line = this.getLineOfOffset(evt.getOffset());
        if (count == 0) {
            this.painter.invalidateLine(line);
        }
        // do magic stuff
        else if (line < this.firstLine) {
            this.setFirstLine(this.firstLine + count);
        }
        // end of magic stuff
        else {
            this.painter.invalidateLineRange(line, this.firstLine + this.visibleLines);
            this.updateScrollBars();
        }
    }

    class ScrollLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(final String name, final Component comp) {
            switch (name) {
                case JEditTextArea.CENTER -> this.center = comp;
                case JEditTextArea.RIGHT -> this.right = comp;
                case JEditTextArea.BOTTOM -> this.bottom = comp;
                case JEditTextArea.LEFT_OF_SCROLLBAR -> this.leftOfScrollBar.addElement(comp);
            }
        }

        @Override
        public void removeLayoutComponent(final Component comp) {
            if (this.center == comp)
                this.center = null;
            if (this.right == comp)
                this.right = null;
            if (this.bottom == comp)
                this.bottom = null;
            else
                this.leftOfScrollBar.removeElement(comp);
        }

        @Override
        public Dimension preferredLayoutSize(final Container parent) {
            final Dimension dim = new Dimension();
            final Insets insets = JEditTextArea.this.getInsets();
            dim.width = insets.left + insets.right;
            dim.height = insets.top + insets.bottom;

            final Dimension centerPref = this.center.getPreferredSize();
            dim.width += centerPref.width;
            dim.height += centerPref.height;
            final Dimension rightPref = this.right.getPreferredSize();
            dim.width += rightPref.width;
            final Dimension bottomPref = this.bottom.getPreferredSize();
            dim.height += bottomPref.height;

            return dim;
        }

        @Override
        public Dimension minimumLayoutSize(final Container parent) {
            final Dimension dim = new Dimension();
            final Insets insets = JEditTextArea.this.getInsets();
            dim.width = insets.left + insets.right;
            dim.height = insets.top + insets.bottom;

            final Dimension centerPref = this.center.getMinimumSize();
            dim.width += centerPref.width;
            dim.height += centerPref.height;
            final Dimension rightPref = this.right.getMinimumSize();
            dim.width += rightPref.width;
            final Dimension bottomPref = this.bottom.getMinimumSize();
            dim.height += bottomPref.height;

            return dim;
        }

        @Override
        public void layoutContainer(final Container parent) {
            final Dimension size = parent.getSize();
            final Insets insets = parent.getInsets();
            final int itop = insets.top;
            int ileft = insets.left;
            final int ibottom = insets.bottom;
            final int iright = insets.right;

            final int rightWidth = this.right.getPreferredSize().width;
            final int bottomHeight = this.bottom.getPreferredSize().height;
            final int centerWidth = size.width - rightWidth - ileft - iright;
            final int centerHeight = size.height - bottomHeight - itop - ibottom;

            this.center.setBounds(
                    ileft,
                    itop,
                    centerWidth,
                    centerHeight);

            this.right.setBounds(
                    ileft + centerWidth,
                    itop,
                    rightWidth,
                    centerHeight);

            // Lay out all status components, in order
            for (final Component comp : this.leftOfScrollBar) {
                final Dimension dim = comp.getPreferredSize();
                comp.setBounds(ileft,
                        itop + centerHeight,
                        dim.width,
                        bottomHeight);
                ileft += dim.width;
            }

            this.bottom.setBounds(
                    ileft,
                    itop + centerHeight,
                    size.width - rightWidth - ileft - iright,
                    bottomHeight);
        }

        // private members
        private Component center;
        private Component right;
        private Component bottom;
        private final Vector<Component> leftOfScrollBar = new Vector<>();
    }

    static class CaretBlinker implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            if (JEditTextArea.focusedComponent != null
                    && JEditTextArea.focusedComponent.hasFocus())
                JEditTextArea.focusedComponent.blinkCaret();
        }
    }

    protected class MutableCaretEvent extends CaretEvent {
        MutableCaretEvent() {
            super(JEditTextArea.this);
        }

        @Override
        public int getDot() {
            return JEditTextArea.this.getCaretPosition();
        }

        @Override
        public int getMark() {
            return JEditTextArea.this.getMarkPosition();
        }
    }

    class AdjustHandler implements AdjustmentListener {
        @Override
        public void adjustmentValueChanged(final AdjustmentEvent evt) {
            if (!JEditTextArea.this.scrollBarsInitialized)
                return;

            // If this is not done, mousePressed events accumulate
            // and the result is that scrolling doesn't stop after
            // the mouse is released
            SwingUtilities.invokeLater(
                    () -> {
                        if (evt.getAdjustable() == JEditTextArea.this.vertical)
                            JEditTextArea.this.setFirstLine(JEditTextArea.this.vertical.getValue());
                        else
                            JEditTextArea.this.setHorizontalOffset(-JEditTextArea.this.horizontal.getValue());
                    });
        }
    }

    class ComponentHandler extends ComponentAdapter {
        @Override
        public void componentResized(final ComponentEvent evt) {
            JEditTextArea.this.recalculateVisibleLines();
            JEditTextArea.this.scrollBarsInitialized = true;
        }
    }

    protected class DocumentHandler implements DocumentListener {
        @Override
        public void insertUpdate(final DocumentEvent evt) {
            JEditTextArea.this.documentChanged(evt);

            final int offset = evt.getOffset();
            final int length = evt.getLength();

            // If event fired because of undo or redo, select inserted text. DPS 3-May-2010
            if (JEditTextArea.this.unredoing) {
                JEditTextArea.this.select(offset, offset + length);
                return;
            }

            final int newStart;
            final int newEnd;

            if (JEditTextArea.this.selectionStart > offset || (JEditTextArea.this.selectionStart == JEditTextArea.this.selectionEnd && JEditTextArea.this.selectionStart == offset))
                newStart = JEditTextArea.this.selectionStart + length;
            else
                newStart = JEditTextArea.this.selectionStart;

            if (JEditTextArea.this.selectionEnd >= offset)
                newEnd = JEditTextArea.this.selectionEnd + length;
            else
                newEnd = JEditTextArea.this.selectionEnd;
            JEditTextArea.this.select(newStart, newEnd);
        }

        @Override
        public void removeUpdate(final DocumentEvent evt) {
            JEditTextArea.this.documentChanged(evt);

            final int offset = evt.getOffset();
            final int length = evt.getLength();

            // If event fired because of undo or redo, move caret to position of removal.
            // DPS 3-May-2010
            if (JEditTextArea.this.unredoing) {
                JEditTextArea.this.select(offset, offset);
                JEditTextArea.this.setCaretPosition(offset);
                return;
            }

            final int newStart;
            final int newEnd;

            if (JEditTextArea.this.selectionStart > offset) {
                if (JEditTextArea.this.selectionStart > offset + length)
                    newStart = JEditTextArea.this.selectionStart - length;
                else
                    newStart = offset;
            } else
                newStart = JEditTextArea.this.selectionStart;

            if (JEditTextArea.this.selectionEnd > offset) {
                if (JEditTextArea.this.selectionEnd > offset + length)
                    newEnd = JEditTextArea.this.selectionEnd - length;
                else
                    newEnd = offset;
            } else
                newEnd = JEditTextArea.this.selectionEnd;
            JEditTextArea.this.select(newStart, newEnd);
        }

        @Override
        public void changedUpdate(final DocumentEvent evt) {
        }
    }

    class DragHandler implements MouseMotionListener {
        @Override
        public void mouseDragged(final MouseEvent evt) {
            if (JEditTextArea.this.popup != null && JEditTextArea.this.popup.isVisible())
                return;

            JEditTextArea.this.setSelectionRectangular((evt.getModifiersEx()
                    & InputEvent.CTRL_DOWN_MASK) != 0);
            JEditTextArea.this.select(JEditTextArea.this.getMarkPosition(), JEditTextArea.this.xyToOffset(evt.getX(), evt.getY()));
        }

        @Override
        public void mouseMoved(final MouseEvent evt) {
        }
    }

    class FocusHandler implements FocusListener {
        @Override
        public void focusGained(final FocusEvent evt) {
            JEditTextArea.this.setCaretVisible(true);
            JEditTextArea.focusedComponent = JEditTextArea.this;
        }

        @Override
        public void focusLost(final FocusEvent evt) {
            JEditTextArea.this.setCaretVisible(false);
            JEditTextArea.focusedComponent = null;
        }
    }

    // Added by DPS, 5-5-2010. Allows use of mouse wheel to scroll.
    // Scrolling as fast as I could, the most notches I could get in
    // one MouseWheelEvent was 3. Normally it will be 1. Nonetheless,
    // this will scroll up to the number in the event, subject to
    // scrollability of the text in its viewport.
    class MouseWheelHandler implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(final MouseWheelEvent e) {
            final int maxMotion = Math.abs(e.getWheelRotation()) * JEditTextArea.LINES_PER_MOUSE_WHEEL_NOTCH;
            if (e.getWheelRotation() < 0) {
                JEditTextArea.this.setFirstLine(JEditTextArea.this.getFirstLine() - Math.min(maxMotion, JEditTextArea.this.getFirstLine()));
            } else {
                JEditTextArea.this.setFirstLine(JEditTextArea.this.getFirstLine()
                        + (Math.min(maxMotion, Math.max(0, JEditTextArea.this.getLineCount() - (JEditTextArea.this.getFirstLine() + JEditTextArea.this.visibleLines)))));
            }
        }
    }

    class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(final MouseEvent evt) {
            JEditTextArea.this.requestFocus();

            // Focus events not fired sometimes?
            JEditTextArea.this.setCaretVisible(true);
            JEditTextArea.focusedComponent = JEditTextArea.this;

            if ((evt.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0
                    && JEditTextArea.this.popup != null) {
                JEditTextArea.this.popup.show(JEditTextArea.this.painter, evt.getX(), evt.getY());
                return;
            }

            final int line = JEditTextArea.this.yToLine(evt.getY());
            final int offset = JEditTextArea.this.xToOffset(line, evt.getX());
            final int dot = JEditTextArea.this.getLineStartOffset(line) + offset;

            switch (evt.getClickCount()) {
                case 1:
                    this.doSingleClick(evt, line, offset, dot);
                    break;
                case 2:
                    // It uses the bracket matching stuff, so
                    // it can throw a BLE
                    this.doDoubleClick(evt, line, offset, dot);
                    break;
                case 3:
                    this.doTripleClick(evt, line, offset, dot);
                    break;
            }
        }

        private void doSingleClick(final MouseEvent evt, final int line,
                                   final int offset, final int dot) {
            if ((evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                JEditTextArea.this.rectSelect = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                JEditTextArea.this.select(JEditTextArea.this.getMarkPosition(), dot);
            } else
                JEditTextArea.this.setCaretPosition(dot);
        }

        private void doDoubleClick(final MouseEvent evt, final int line,
                                   final int offset, final int dot) {
            // Ignore empty lines
            if (JEditTextArea.this.getLineLength(line) == 0)
                return;

            try {
                int bracket = TextUtilities.findMatchingBracket(
                        JEditTextArea.this.document, Math.max(0, dot - 1));
                if (bracket != -1) {
                    int mark = JEditTextArea.this.getMarkPosition();
                    // Hack
                    if (bracket > mark) {
                        bracket++;
                        mark--;
                    }
                    JEditTextArea.this.select(mark, bracket);
                    return;
                }
            } catch (final BadLocationException bl) {
                JEditTextArea.LOGGER.error("Error finding matching bracket", bl);
            }

            // Ok, it's not a bracket... select the word
            final String lineText = JEditTextArea.this.getLineText(line);
            char ch = lineText.charAt(Math.max(0, offset - 1));

            String noWordSep = (String) JEditTextArea.this.document.getProperty("noWordSep");
            if (noWordSep == null)
                noWordSep = "";

            // If the user clicked on a non-letter char,
            // we select the surrounding non-letters
            final boolean selectNoLetter = (!Character
                    .isLetterOrDigit(ch)
                    && noWordSep.indexOf(ch) == -1);

            int wordStart = 0;

            for (int i = offset - 1; i >= 0; i--) {
                ch = lineText.charAt(i);
                if (selectNoLetter ^ (!Character
                        .isLetterOrDigit(ch) &&
                        noWordSep.indexOf(ch) == -1)) {
                    wordStart = i + 1;
                    break;
                }
            }

            int wordEnd = lineText.length();
            for (int i = offset; i < lineText.length(); i++) {
                ch = lineText.charAt(i);
                if (selectNoLetter ^ (!Character
                        .isLetterOrDigit(ch) &&
                        noWordSep.indexOf(ch) == -1)) {
                    wordEnd = i;
                    break;
                }
            }

            final int lineStart = JEditTextArea.this.getLineStartOffset(line);
            JEditTextArea.this.select(lineStart + wordStart, lineStart + wordEnd);
        }

        private void doTripleClick(final MouseEvent evt, final int line,
                                   final int offset, final int dot) {
            JEditTextArea.this.select(JEditTextArea.this.getLineStartOffset(line), JEditTextArea.this.getLineEndOffset(line) - 1);
        }
    }

    class CaretUndo extends AbstractUndoableEdit {
        private int start;
        private int end;

        CaretUndo(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean isSignificant() {
            return false;
        }

        @Override
        public String getPresentationName() {
            return "caret move";
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();

            JEditTextArea.this.select(this.start, this.end);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();

            JEditTextArea.this.select(this.start, this.end);
        }

        @Override
        public boolean addEdit(final UndoableEdit edit) {
            if (edit instanceof final CaretUndo cedit) {
                this.start = cedit.start;
                this.end = cedit.end;
                cedit.die();

                return true;
            } else
                return false;
        }
    }

    /**
     * Return any relevant tool tip text for token at specified position. Keyword
     * match
     * must be exact. DPS 24-May-2010
     *
     * @param x x-coordinate of current position
     * @param y y-coordinate of current position
     * @return String containing appropriate tool tip text. Possibly HTML-encoded.
     */
    // Is used for tool tip only (not popup menu)
    public String getSyntaxSensitiveToolTipText(final int x, final int y) {
        final StringBuilder result;
        final int line = this.yToLine(y);
        final ArrayList<PopupHelpItem> matches = this.getSyntaxSensitiveHelpAtLineOffset(line, this.xToOffset(line, x), true);
        if (matches == null) {
            return null;
        }
        final int length = PopupHelpItem.maxExampleLength(matches) + 2;
        result = new StringBuilder("<html>");
        for (int i = 0; i < matches.size(); i++) {
            final PopupHelpItem match = matches.get(i);
            result.append((i == 0) ? "" : "<br>").append("<tt>").append(match.getExamplePaddedToLength(length).replaceAll(" ", "&nbsp;")).append("</tt>").append(match.getDescription());
        }
        return result + "</html>";
    }

    /**
     * Constructs string for auto-indent feature. Returns empty string
     * if auto-intent is disabled or if line has no leading white space.
     * Uses getLeadingWhiteSpace(). Is used by InputHandler when processing
     * key press for Enter key. DPS 31-Dec-2010
     *
     * @return String containing auto-indent characters to be inserted into text
     */
    public String getAutoIndent() {
        return (Globals.getSettings().getBooleanSetting(Settings.Bool.AUTO_INDENT)) ? this.getLeadingWhiteSpace() : "";
    }

    /**
     * Makes a copy of leading white space (tab or space) from the current line and
     * returns it. DPS 31-Dec-2010
     *
     * @return String containing leading white space of current line. Empty string
     * if none.
     */
    public String getLeadingWhiteSpace() {
        final int line = this.getCaretLine();
        final int lineLength = this.getLineLength(line);
        String indent = "";
        if (lineLength > 0) {
            final String text = this.getText(this.getLineStartOffset(line), lineLength);
            for (int position = 0; position < text.length(); position++) {
                final char character = text.charAt(position);
                if (character == '\t' || character == ' ') {
                    indent += character;
                } else {
                    break;
                }
            }
        }
        return indent;
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Get relevant help information at specified position. Returns ArrayList of
    // PopupHelpItem with one per match, or null if no matches.
    // The "exact" parameter is set depending on whether the match has to be
    // exact or whether a prefix match will do. The token "s" will not match
    // any instruction names if exact is true, but will match "sw", "sh", etc
    // if exact is false. The former is helpful for mouse-movement-based tool
    // tips (this is what you have). The latter is helpful for caret-based tool
    // tips (this is what you can do).
    private ArrayList<PopupHelpItem> getSyntaxSensitiveHelpAtLineOffset(final int line, final int offset, final boolean exact) {
        ArrayList<PopupHelpItem> matches = null;
        final TokenMarker tokenMarker = this.getTokenMarker();
        if (tokenMarker != null) {
            final Segment lineSegment = new Segment();
            this.getLineText(line, lineSegment); // fill segment with info from this line
            Token tokens = tokenMarker.markTokens(lineSegment, line);
            final Token tokenList = tokens;
            int tokenOffset = 0;
            Token tokenAtOffset = null;
            for (; ; ) {
                final byte id = tokens.id;
                if (id == Token.END)
                    break;
                final int length = tokens.length;
                if (offset > tokenOffset && offset <= tokenOffset + length) {
                    tokenAtOffset = tokens;
                    break;
                }
                tokenOffset += length;
                tokens = tokens.next;
            }
            if (tokenAtOffset != null) {
                final String tokenText = lineSegment.toString().substring(tokenOffset, tokenOffset + tokenAtOffset.length);
                if (exact) {
                    matches = tokenMarker.getTokenExactMatchHelp(tokenAtOffset, tokenText);
                } else {
                    matches = tokenMarker.getTokenPrefixMatchHelp(lineSegment.toString(), tokenList, tokenAtOffset,
                            tokenText);
                }
            }
        }
        return matches;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Compose and display syntax-sensitive help. Typically invoked upon typing a
    //////////////////////////////////////////////////////////////////////////////////// key.
    // Results in popup menu. Is not used for creating tool tips.
    private void applySyntaxSensitiveHelp() {
        if (!Globals.getSettings().getBooleanSetting(Settings.Bool.POPUP_INSTRUCTION_GUIDANCE)) {
            return;
        }
        final int line = this.getCaretLine();
        final int lineStart = this.getLineStartOffset(line);
        final int offset = Math.max(1, Math.min(this.getLineLength(line),
                this.getCaretPosition() - lineStart));
        final ArrayList<PopupHelpItem> helpItems = this.getSyntaxSensitiveHelpAtLineOffset(line, offset, false);
        if (helpItems == null && this.popupMenu != null) {
            this.popupMenu.setVisible(false);
            this.popupMenu = null;
        }
        if (helpItems != null) {
            this.popupMenu = new JPopupMenu();
            final int length = PopupHelpItem.maxExampleLength(helpItems) + 2;
            for (final PopupHelpItem item : helpItems) {
                final JMenuItem menuItem = new JMenuItem(
                        "<html><tt>" + item.getExamplePaddedToLength(length).replaceAll(" ", "&nbsp;") + "</tt>"
                                + item.getDescription() + "</html>");
                if (item.getExact()) {
                    // The instruction name is completed so the role of the popup changes
                    // to that of floating help to assist in operand specification.
                    menuItem.setSelected(false);
                    // Want menu item to be disabled but that causes rendered text to be hard to
                    // see.
                    // Spent a couple hours on workaround with no success. The UI uses
                    // UIManager.get("MenuItem.disabledForeground") property to determine rendering
                    // color but this is done each time the text is rendered (paintText). There is
                    // no setter for the menu item itself. The UIManager property is used for all
                    // menus not just the editor's popup help menu, so you can't just set the
                    // disabled
                    // foreground color to, say, black and leave it. Tried several techniques
                    // without
                    // success. The only solution I found was a hack: writing a BasicMenuItem UI
                    // subclass that consists of hacked override of its paintText() method. But even
                    // this required use of "SwingUtilities2" class which has been deprecated for
                    // years
                    // So in the end I decided just to leave the menu item enabled. It will
                    // highlight
                    // but does nothing if selected. DPS 11-July-2014

                    // menuItem.setEnabled(false);
                } else {
                    // Typing of instruction/directive name is still in progress; the action
                    // listener
                    // will complete it when its menu item is selected.
                    menuItem.addActionListener(new PopupHelpActionListener(item.getTokenText(), item.getExample()));
                }
                this.popupMenu.add(menuItem);
            }
            this.popupMenu.pack();
            final int y = this.lineToY(line);
            final int x = this.offsetToX(line, offset);
            final int height = this.painter.getFontMetrics(this.painter.getFont()).getHeight();
            final int width = this.painter.getFontMetrics(this.painter.getFont()).charWidth('w');
            final int menuXLoc = x + width + width + width;
            final int menuYLoc = y + height + height; // display below;
            // Modified to always display popup BELOW the current line.
            // This was done in response to negative student feedback about
            // the popup blocking information they needed to (e.g. operands from
            // previous instructions). Note that if menu is long enough and
            // current cursor position is low enough, the menu will bottom out at the
            // bottom of the screen and extend above the current line. DPS 23-Dec-2010
            this.popupMenu.show(this, menuXLoc, menuYLoc);
            this.requestFocusInWindow(); // get cursor back from the menu
        }
    }

    // Carries out the instruction/directive completion when popup menu
    // item is selected.
    private class PopupHelpActionListener implements ActionListener {
        private final String tokenText;
        private final String text;

        public PopupHelpActionListener(final String tokenText, final String text) {
            this.tokenText = tokenText;
            this.text = text.split(" ")[0];
        }

        // Completion action will insert either a tab or space character following the
        // completed instruction mnemonic. Inserts a tab if tab key was pressed;
        // space otherwise. Get this information from the ActionEvent.
        @Override
        public void actionPerformed(final ActionEvent e) {
            final String insert = (e.getActionCommand().charAt(0) == '\t') ? "\t" : " ";
            if (this.tokenText.length() >= this.text.length()) {
                JEditTextArea.this.overwriteSetSelectedText(insert);
            } else {
                JEditTextArea.this.overwriteSetSelectedText(this.text.substring(this.tokenText.length()) + insert);
            }
        }
    }

    private void checkAutoIndent(final KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            final int line = this.getCaretLine();
            if (line <= 0)
                return;
            final int previousLine = line - 1;
            final int previousLineLength = this.getLineLength(previousLine);
            if (previousLineLength <= 0)
                return;
            final String previous = this.getText(this.getLineStartOffset(previousLine), previousLineLength);
            String indent = "";
            for (int position = 0; position < previous.length(); position++) {
                final char character = previous.charAt(position);
                if (character == '\t' || character == ' ') {
                    indent += character;
                } else {
                    break;
                }
            }
            this.overwriteSetSelectedText(indent);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Called after processing a Key Pressed event. Will make popup menu disappear
    //////////////////////////////////////////////////////////////////////////////////// if
    // Enter or Escape keys pressed. Will update if Backspace or Delete pressed.
    // Not really concerned with modifiers here.
    private void checkPopupMenu(final KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE || evt.getKeyCode() == KeyEvent.VK_DELETE)
            this.applySyntaxSensitiveHelp();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Called before processing Key Pressed event. If popup menu is visible, will
    //////////////////////////////////////////////////////////////////////////////////// process
    // tab and enter keys to select from the menu, and arrow keys to traverse the
    //////////////////////////////////////////////////////////////////////////////////// menu.
    private boolean checkPopupCompletion(final KeyEvent evt) {
        if ((evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
                && this.popupMenu != null && this.popupMenu.isVisible() && this.popupMenu.getComponentCount() > 0) {
            final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
            if (path.length < 1 || !(path[path.length - 1] instanceof AbstractButton))
                return false;
            final AbstractButton item = (AbstractButton) path[path.length - 1].getComponent();
            if (item.isEnabled()) {
                int index = this.popupMenu.getComponentIndex(item);
                if (index < 0)
                    return false;
                if (evt.getKeyCode() == KeyEvent.VK_UP) {
                    index = (index == 0) ? this.popupMenu.getComponentCount() - 1 : index - 1;
                } else {
                    index = (index == this.popupMenu.getComponentCount() - 1) ? 0 : index + 1;
                }
                // Neither popupMenu.setSelected() nor
                // popupMenu.getSelectionModel().setSelectedIndex()
                // have the desired effect (changing the menu item selected). Found references
                // to
                // this in a Sun forum.
                // http://forums.sun.com/thread.jspa?forumID=57&threadID=641745
                // The solution, as shown here, is to use invokeLater.
                final MenuElement[] newPath = new MenuElement[2];
                newPath[0] = path[0];
                newPath[1] = (MenuElement) this.popupMenu.getComponent(index);
                SwingUtilities.invokeLater(
                        () -> MenuSelectionManager.defaultManager().setSelectedPath(newPath));
                return true;
            } else {
                return false;
            }
        }
        if ((evt.getKeyCode() == KeyEvent.VK_TAB || evt.getKeyCode() == KeyEvent.VK_ENTER)
                && this.popupMenu != null && this.popupMenu.isVisible() && this.popupMenu.getComponentCount() > 0) {
            final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
            if (path.length < 1 || !(path[path.length - 1] instanceof AbstractButton))
                return false;
            final AbstractButton item = (AbstractButton) path[path.length - 1].getComponent();
            if (item.isEnabled()) {
                final ActionListener[] listeners = item.getActionListeners();
                if (listeners.length > 0) {
                    listeners[0].actionPerformed(new ActionEvent(item, ActionEvent.ACTION_FIRST,
                            (evt.getKeyCode() == KeyEvent.VK_TAB) ? "\t" : " "));
                    return true;
                }
            }
        }
        return false;
    }

    static {
        caretTimer = new Timer(500, new CaretBlinker());
        JEditTextArea.caretTimer.setInitialDelay(500);
        JEditTextArea.caretTimer.start();
    }
}
