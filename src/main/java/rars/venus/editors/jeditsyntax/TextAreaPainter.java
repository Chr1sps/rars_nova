/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999 Slava Pestov
 *
 * 08/05/2002	Cursor (caret) rendering fixed for JDK 1.4 (Anonymous)
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import rars.venus.editors.jeditsyntax.tokenmarker.Token;
import rars.venus.editors.jeditsyntax.tokenmarker.TokenMarker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 *
 * @author Slava Pestov
 * @version $Id: TextAreaPainter.java,v 1.24 1999/12/13 03:40:30 sp Exp $
 */
public class TextAreaPainter extends JComponent implements TabExpander {
    private static final Logger LOGGER = LogManager.getLogger(TextAreaPainter.class);

    /**
     * Creates a new repaint manager. This should be not be called
     * directly.
     *
     * @param textArea a {@link JEditTextArea} object
     * @param defaults a {@link TextAreaDefaults} object
     */
    public TextAreaPainter(final JEditTextArea textArea, final TextAreaDefaults defaults) {
        this.textArea = textArea;

        this.setAutoscrolls(true);
        this.setDoubleBuffered(true);
        this.setOpaque(true);

        ToolTipManager.sharedInstance().registerComponent(this);

        this.currentLine = new Segment();
        this.currentLineIndex = -1;

        this.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        this.setFont(new Font("Courier New" /* "Monospaced" */, Font.PLAIN, 14));
        this.setForeground(Color.black);
        this.setBackground(Color.white);

        this.tabSizeChars = defaults.tabSize;
        this.blockCaret = defaults.blockCaret;
        this.styles = defaults.styles;
        this.cols = defaults.cols;
        this.rows = defaults.rows;
        this.caretColor = defaults.caretColor;
        this.selectionColor = defaults.selectionColor;
        this.lineHighlightColor = defaults.lineHighlightColor;
        this.lineHighlight = defaults.lineHighlight;
        this.bracketHighlightColor = defaults.bracketHighlightColor;
        this.bracketHighlight = defaults.bracketHighlight;
        this.paintInvalid = defaults.paintInvalid;
        this.eolMarkerColor = defaults.eolMarkerColor;
        this.eolMarkers = defaults.eolMarkers;
    }


    /**
     * Fetch the tab size in characters. DPS 12-May-2010.
     *
     * @return int tab size in characters
     */
    public int getTabSize() {
        return this.tabSizeChars;
    }

    /**
     * Set the tab size in characters. DPS 12-May-2010.
     * Originally it was fixed at PlainDocument property
     * value (8).
     *
     * @param size tab size in characters
     */
    public void setTabSize(final int size) {
        this.tabSizeChars = size;
    }

    /**
     * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
     * will be used to paint tokens with id = <i>n</i>.
     *
     * @return an array of {@link SyntaxStyle} objects
     */
    public final SyntaxStyle[] getStyles() {
        return this.styles;
    }

    /**
     * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
     * will be used to paint tokens with id = <i>n</i>.
     *
     * @param styles The syntax styles
     */
    public final void setStyles(final SyntaxStyle[] styles) {
        this.styles = styles;
        this.repaint();
    }

    /**
     * Returns the caret color.
     *
     * @return a {@link java.awt.Color} object
     */
    public final Color getCaretColor() {
        return this.caretColor;
    }

    /**
     * Sets the caret color.
     *
     * @param caretColor The caret color
     */
    public final void setCaretColor(final Color caretColor) {
        this.caretColor = caretColor;
        this.invalidateSelectedLines();
    }

    /**
     * Returns the selection color.
     *
     * @return a {@link java.awt.Color} object
     */
    public final Color getSelectionColor() {
        return this.selectionColor;
    }

    /**
     * Sets the selection color.
     *
     * @param selectionColor The selection color
     */
    public final void setSelectionColor(final Color selectionColor) {
        this.selectionColor = selectionColor;
        this.invalidateSelectedLines();
    }

    /**
     * Returns the line highlight color.
     *
     * @return a {@link java.awt.Color} object
     */
    public final Color getLineHighlightColor() {
        return this.lineHighlightColor;
    }

    /**
     * Sets the line highlight color.
     *
     * @param lineHighlightColor The line highlight color
     */
    public final void setLineHighlightColor(final Color lineHighlightColor) {
        this.lineHighlightColor = lineHighlightColor;
        this.invalidateSelectedLines();
    }

    /**
     * Returns true if line highlight is enabled, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isLineHighlightEnabled() {
        return this.lineHighlight;
    }

    /**
     * Enables or disables current line highlighting.
     *
     * @param lineHighlight True if current line highlight should be enabled,
     *                      false otherwise
     */
    public final void setLineHighlightEnabled(final boolean lineHighlight) {
        this.lineHighlight = lineHighlight;
        this.invalidateSelectedLines();
    }

    /**
     * Returns the bracket highlight color.
     *
     * @return a {@link java.awt.Color} object
     */
    public final Color getBracketHighlightColor() {
        return this.bracketHighlightColor;
    }

    /**
     * Sets the bracket highlight color.
     *
     * @param bracketHighlightColor The bracket highlight color
     */
    public final void setBracketHighlightColor(final Color bracketHighlightColor) {
        this.bracketHighlightColor = bracketHighlightColor;
        this.invalidateLine(this.textArea.getBracketLine());
    }

    /**
     * Returns true if bracket highlighting is enabled, false otherwise.
     * When bracket highlighting is enabled, the bracket matching the
     * one before the caret (if any) is highlighted.
     *
     * @return a boolean
     */
    public final boolean isBracketHighlightEnabled() {
        return this.bracketHighlight;
    }

    /**
     * Enables or disables bracket highlighting.
     * When bracket highlighting is enabled, the bracket matching the
     * one before the caret (if any) is highlighted.
     *
     * @param bracketHighlight True if bracket highlighting should be
     *                         enabled, false otherwise
     */
    public final void setBracketHighlightEnabled(final boolean bracketHighlight) {
        this.bracketHighlight = bracketHighlight;
        this.invalidateLine(this.textArea.getBracketLine());
    }

    /**
     * Returns true if the caret should be drawn as a block, false otherwise.
     *
     * @return a boolean
     */
    public final boolean isBlockCaretEnabled() {
        return this.blockCaret;
    }

    /**
     * Sets if the caret should be drawn as a block, false otherwise.
     *
     * @param blockCaret True if the caret should be drawn as a block,
     *                   false otherwise.
     */
    public final void setBlockCaretEnabled(final boolean blockCaret) {
        this.blockCaret = blockCaret;
        this.invalidateSelectedLines();
    }

    /**
     * Returns the EOL marker color.
     *
     * @return a {@link java.awt.Color} object
     */
    public final Color getEOLMarkerColor() {
        return this.eolMarkerColor;
    }

    /**
     * Sets the EOL marker color.
     *
     * @param eolMarkerColor The EOL marker color
     */
    public final void setEOLMarkerColor(final Color eolMarkerColor) {
        this.eolMarkerColor = eolMarkerColor;
        this.repaint();
    }

    /**
     * Returns true if EOL markers are drawn, false otherwise.
     *
     * @return a boolean
     */
    public final boolean getEOLMarkersPainted() {
        return this.eolMarkers;
    }

    /**
     * Sets if EOL markers are to be drawn.
     *
     * @param eolMarkers True if EOL markers should be drawn, false otherwise
     */
    public final void setEOLMarkersPainted(final boolean eolMarkers) {
        this.eolMarkers = eolMarkers;
        this.repaint();
    }

    /**
     * Returns true if invalid lines are painted as red tildes (~),
     * false otherwise.
     *
     * @return a boolean
     */
    public boolean getInvalidLinesPainted() {
        return this.paintInvalid;
    }

    /**
     * Sets if invalid lines are to be painted as red tildes.
     *
     * @param paintInvalid True if invalid lines should be drawn, false otherwise
     */
    public void setInvalidLinesPainted(final boolean paintInvalid) {
        this.paintInvalid = paintInvalid;
    }

    /**
     * Adds a custom highlight painter.
     *
     * @param highlight The highlight
     */
    public void addCustomHighlight(final Highlight highlight) {
        highlight.init(this.textArea, this.highlights);
        this.highlights = highlight;
    }

    /**
     * Highlight interface.
     */
    public interface Highlight {
        /**
         * Called after the highlight painter has been added.
         *
         * @param textArea The text area
         * @param next     The painter this one should delegate to
         */
        void init(JEditTextArea textArea, Highlight next);

        /**
         * This should paint the highlight and delgate to the
         * next highlight painter.
         *
         * @param gfx  The graphics context
         * @param line The line number
         * @param y    The y co-ordinate of the line
         */
        void paintHighlight(Graphics gfx, int line, int y);

        /**
         * Returns the tool tip to display at the specified
         * location. If this highlighter doesn't know what to
         * display, it should delegate to the next highlight
         * painter.
         *
         * @param evt The mouse event
         */
        String getToolTipText(MouseEvent evt);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the tool tip to display at the specified location.
     */
    @Override
    public String getToolTipText(final MouseEvent evt) {
        // if(highlights != null)
        // return highlights.getToolTipText(evt);
        // else
        // return null;
        if (this.highlights != null) {
            return this.highlights.getToolTipText(evt);
        } else if (this.textArea.getTokenMarker() == null) {
            return null;
        } else {
            return this.textArea.getSyntaxSensitiveToolTipText(evt.getX(), evt.getY());
        }
        // int line = yToLine(evt.getY());
        // int offset = xToOffset(line,evt.getX());
        // {
        // if (evt instanceof InstructionMouseEvent) {
        // System.out.println("get Tool Tip Text for InstructionMouseEvent");
        // return "Instruction: "+ ((InstructionMouseEvent)evt).getLine().toString();
        // }
        // else {
        // return "Not a fake?";//null;
        // }
        // }
    }

    /**
     * Returns the font metrics used by this component.
     *
     * @return a {@link java.awt.FontMetrics} object
     */
    public FontMetrics getFontMetrics() {
        return this.fm;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the font for this component. This is overridden to update the
     * cached font metrics and to recalculate which lines are visible.
     */
    @Override
    public void setFont(final Font font) {
        super.setFont(font);
        final var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final var graphics = img.getGraphics();
        this.fm = graphics.getFontMetrics(font);
        graphics.dispose();
        this.textArea.recalculateVisibleLines();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Repaints the text.
     */
    @Override
    public void paint(final Graphics gfx) {

        // Added 4/6/10 DPS to set antialiasing for text rendering - smoother letters
        // Second one says choose algorithm for quality over speed
        ((Graphics2D) gfx).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D) gfx).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        this.tabSize = this.fm.charWidth(' ') * this.tabSizeChars; // was:
        // ((Integer)textArea.getDocument().getProperty(PlainDocument.tabSizeAttribute)).intValue();

        final Rectangle clipRect = gfx.getClipBounds();

        gfx.setColor(this.getBackground());
        gfx.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

        // We don't use yToLine() here because that method doesn't
        // return lines past the end of the document
        final int height = this.fm.getHeight();
        final int firstLine = this.textArea.getFirstLine();
        final int firstInvalid = firstLine + clipRect.y / height;
        // Because the clipRect's height is usually an even multiple
        // of the font height, we subtract 1 from it, otherwise one
        // too many lines will always be painted.
        final int lastInvalid = firstLine + (clipRect.y + clipRect.height - 1) / height;

        try {
            final TokenMarker tokenMarker = ((SyntaxDocument) this.textArea.getDocument())
                    .getTokenMarker();
            final int x = this.textArea.getHorizontalOffset();

            for (int line = firstInvalid; line <= lastInvalid; line++) {
                this.paintLine(gfx, tokenMarker, line, x);
            }

            if (tokenMarker != null && tokenMarker.isNextLineRequested()) {
                final int h = clipRect.y + clipRect.height;
                this.repaint(0, h, this.getWidth(), this.getHeight() - h);
            }
        } catch (final Exception e) {
            LOGGER.error("Error repainting line range {{},{}}: {}", firstInvalid, lastInvalid, e);
        }
    }

    /**
     * Marks a line as needing a repaint.
     *
     * @param line The line to invalidate
     */
    public final void invalidateLine(final int line) {
        this.repaint(0, this.textArea.lineToY(line) + this.fm.getMaxDescent() + this.fm.getLeading(),
                this.getWidth(), this.fm.getHeight());
    }

    /**
     * Marks a range of lines as needing a repaint.
     *
     * @param firstLine The first line to invalidate
     * @param lastLine  The last line to invalidate
     */
    public final void invalidateLineRange(final int firstLine, final int lastLine) {
        this.repaint(0, this.textArea.lineToY(firstLine) + this.fm.getMaxDescent() + this.fm.getLeading(),
                this.getWidth(), (lastLine - firstLine + 1) * this.fm.getHeight());
    }

    /**
     * Repaints the lines containing the selection.
     */
    public final void invalidateSelectedLines() {
        this.invalidateLineRange(this.textArea.getSelectionStartLine(),
                this.textArea.getSelectionEndLine());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation of TabExpander interface. Returns next tab stop after
     * a specified point.
     */
    @Override
    public float nextTabStop(final float x, final int tabOffset) {
        final int offset = this.textArea.getHorizontalOffset();
        final int ntabs = ((int) x - offset) / this.tabSize;
        return (ntabs + 1) * this.tabSize + offset;
    }

    /**
     * Returns the painter's preferred size.
     *
     * @return a {@link java.awt.Dimension} object
     */
    @Override
    public Dimension getPreferredSize() {
        final Dimension dim = new Dimension();
        dim.width = this.fm.charWidth('w') * this.cols;
        dim.height = this.fm.getHeight() * this.rows;
        return dim;
    }

    /**
     * Returns the painter's minimum size.
     *
     * @return a {@link java.awt.Dimension} object
     */
    @Override
    public Dimension getMinimumSize() {
        return this.getPreferredSize();
    }

    // package-private members
    int currentLineIndex;
    Token currentLineTokens;
    final Segment currentLine;

    // protected members
    protected final JEditTextArea textArea;

    protected SyntaxStyle[] styles;
    protected Color caretColor;
    protected Color selectionColor;
    protected Color lineHighlightColor;
    protected Color bracketHighlightColor;
    protected Color eolMarkerColor;

    protected boolean blockCaret;
    protected boolean lineHighlight;
    protected boolean bracketHighlight;
    protected boolean paintInvalid;
    protected boolean eolMarkers;
    protected final int cols;
    protected final int rows;

    protected int tabSize, tabSizeChars;
    protected FontMetrics fm;

    protected Highlight highlights;

    /**
     * <p>paintLine.</p>
     *
     * @param gfx         a {@link java.awt.Graphics} object
     * @param tokenMarker a {@link TokenMarker} object
     * @param line        a int
     * @param x           a int
     */
    protected void paintLine(final Graphics gfx, final TokenMarker tokenMarker,
                             final int line, final int x) {// System.out.println("paintLine "+ (++count));
        final Font defaultFont = this.getFont();
        final Color defaultColor = this.getForeground();

        this.currentLineIndex = line;
        final int y = this.textArea.lineToY(line);

        if (line < 0 || line >= this.textArea.getLineCount()) {
            if (this.paintInvalid) {
                this.paintHighlight(gfx, line, y);
                this.styles[Token.INVALID].setGraphicsFlags(gfx, defaultFont);
                gfx.drawString("~", 0, y + this.fm.getHeight());
            }
        } else if (tokenMarker == null) {
            this.paintPlainLine(gfx, line, defaultFont, defaultColor, x, y);
        } else {
            this.paintSyntaxLine(gfx, tokenMarker, line, defaultFont,
                    defaultColor, x, y);
        }
    }

    /**
     * <p>paintPlainLine.</p>
     *
     * @param gfx          a {@link java.awt.Graphics} object
     * @param line         a int
     * @param defaultFont  a {@link java.awt.Font} object
     * @param defaultColor a {@link java.awt.Color} object
     * @param x            a int
     * @param y            a int
     */
    protected void paintPlainLine(final Graphics gfx, final int line, final Font defaultFont,
                                  final Color defaultColor, int x, int y) {
        this.paintHighlight(gfx, line, y);
        this.textArea.getLineText(line, this.currentLine);

        gfx.setFont(defaultFont);
        gfx.setColor(defaultColor);

        y += this.fm.getHeight();
        x = (int) Utilities.drawTabbedText(this.currentLine, (float) x, (float) y, (Graphics2D) gfx, this, 0);

        if (this.eolMarkers) {
            gfx.setColor(this.eolMarkerColor);
            gfx.drawString(".", x, y);
        }
    }

    // private int count=0;

    /**
     * <p>paintSyntaxLine.</p>
     *
     * @param gfx          a {@link java.awt.Graphics} object
     * @param tokenMarker  a {@link TokenMarker} object
     * @param line         a int
     * @param defaultFont  a {@link java.awt.Font} object
     * @param defaultColor a {@link java.awt.Color} object
     * @param x            a int
     * @param y            a int
     */
    protected void paintSyntaxLine(final Graphics gfx, final TokenMarker tokenMarker,
                                   final int line, final Font defaultFont, final Color defaultColor, int x, int y) {// System.out.println("paintSyntaxLine line
        // "+ line);
        this.textArea.getLineText(this.currentLineIndex, this.currentLine);
        this.currentLineTokens = tokenMarker.markTokens(this.currentLine,
                this.currentLineIndex);

        this.paintHighlight(gfx, line, y);

        gfx.setFont(defaultFont);
        gfx.setColor(defaultColor);
        y += this.fm.getHeight();
        x = SyntaxUtilities.paintSyntaxLine(this.currentLine,
                this.currentLineTokens, this.styles, this, gfx, x, y);
        // count++;
        // if (count % 100 == 10) {
        // textArea.setToolTipText("Setting Text at Count of "+count);
        // System.out.println("set tool tip");
        // }
        // if (count % 100 == 60) {
        // textArea.setToolTipText(null);System.out.println("reset tool tip");
        // }
        // System.out.println("SyntaxUtilities.paintSyntaxLine "+ (++count));
        if (this.eolMarkers) {
            gfx.setColor(this.eolMarkerColor);
            gfx.drawString(".", x, y);
        }
    }

    /**
     * <p>paintHighlight.</p>
     *
     * @param gfx  a {@link java.awt.Graphics} object
     * @param line a int
     * @param y    a int
     */
    protected void paintHighlight(final Graphics gfx, final int line, final int y) {// System.out.println("paintHighlight "+ (++count));
        if (line >= this.textArea.getSelectionStartLine()
                && line <= this.textArea.getSelectionEndLine())
            this.paintLineHighlight(gfx, line, y);

        if (this.highlights != null)
            this.highlights.paintHighlight(gfx, line, y);

        if (this.bracketHighlight && line == this.textArea.getBracketLine())
            this.paintBracketHighlight(gfx, line, y);

        if (line == this.textArea.getCaretLine())
            this.paintCaret(gfx, line, y);
    }

    /**
     * <p>paintLineHighlight.</p>
     *
     * @param gfx  a {@link java.awt.Graphics} object
     * @param line a int
     * @param y    a int
     */
    protected void paintLineHighlight(final Graphics gfx, final int line, int y) {// System.out.println("paintLineHighlight "+
        // (++count));
        final int height = this.fm.getHeight();
        y += this.fm.getLeading() + this.fm.getMaxDescent();

        final int selectionStart = this.textArea.getSelectionStart();
        final int selectionEnd = this.textArea.getSelectionEnd();

        if (selectionStart == selectionEnd) {
            if (this.lineHighlight) {
                gfx.setColor(this.lineHighlightColor);
                gfx.fillRect(0, y, this.getWidth(), height);
            }
        } else {
            gfx.setColor(this.selectionColor);

            final int selectionStartLine = this.textArea.getSelectionStartLine();
            final int selectionEndLine = this.textArea.getSelectionEndLine();
            final int lineStart = this.textArea.getLineStartOffset(line);

            final int x1;
            int x2;
            if (this.textArea.isSelectionRectangular()) {
                final int lineLen = this.textArea.getLineLength(line);
                x1 = this.textArea._offsetToX(line, Math.min(lineLen,
                        selectionStart - this.textArea.getLineStartOffset(
                                selectionStartLine)));
                x2 = this.textArea._offsetToX(line, Math.min(lineLen,
                        selectionEnd - this.textArea.getLineStartOffset(
                                selectionEndLine)));
                if (x1 == x2)
                    x2++;
            } else if (selectionStartLine == selectionEndLine) {
                x1 = this.textArea._offsetToX(line,
                        selectionStart - lineStart);
                x2 = this.textArea._offsetToX(line,
                        selectionEnd - lineStart);
            } else if (line == selectionStartLine) {
                x1 = this.textArea._offsetToX(line,
                        selectionStart - lineStart);
                x2 = this.getWidth();
            } else if (line == selectionEndLine) {
                x1 = 0;
                x2 = this.textArea._offsetToX(line,
                        selectionEnd - lineStart);
            } else {
                x1 = 0;
                x2 = this.getWidth();
            }

            // "inlined" min/max()
            gfx.fillRect(Math.min(x1, x2), y, x1 > x2 ? (x1 - x2) : (x2 - x1), height);
        }

    }

    /**
     * <p>paintBracketHighlight.</p>
     *
     * @param gfx  a {@link java.awt.Graphics} object
     * @param line a int
     * @param y    a int
     */
    protected void paintBracketHighlight(final Graphics gfx, final int line, int y) {
        final int position = this.textArea.getBracketPosition();
        if (position == -1)
            return;
        y += this.fm.getLeading() + this.fm.getMaxDescent();
        final int x = this.textArea._offsetToX(line, position);
        gfx.setColor(this.bracketHighlightColor);
        // Hack!!! Since there is no fast way to get the character
        // from the bracket matching routine, we use ( since all
        // brackets probably have the same width anyway
        gfx.drawRect(x, y, this.fm.charWidth('(') - 1,
                this.fm.getHeight() - 1);
    }

    /**
     * <p>paintCaret.</p>
     *
     * @param gfx  a {@link java.awt.Graphics} object
     * @param line a int
     * @param y    a int
     */
    protected void paintCaret(final Graphics gfx, final int line, int y) {
        if (this.textArea.isCaretVisible()) {
            final int offset = this.textArea.getCaretPosition()
                    - this.textArea.getLineStartOffset(line);
            final int caretX = this.textArea._offsetToX(line, offset);
            final int caretWidth = ((this.blockCaret ||
                    this.textArea.isOverwriteEnabled()) ? this.fm.charWidth('w') : 1);
            y += this.fm.getLeading() + this.fm.getMaxDescent();
            final int height = this.fm.getHeight();

            gfx.setColor(this.caretColor);

            if (this.textArea.isOverwriteEnabled()) {
                gfx.fillRect(caretX, y + height - 1,
                        caretWidth, 1);
            } else {
                gfx.drawRect(caretX, y, caretWidth, height - 1);
            }
        }
    }
}
