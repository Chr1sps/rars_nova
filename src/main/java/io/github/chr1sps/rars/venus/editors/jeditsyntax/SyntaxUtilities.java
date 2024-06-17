/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package io.github.chr1sps.rars.venus.editors.jeditsyntax;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.tokenmarker.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Class with several utility functions used by jEdit's syntax colorizing
 * subsystem.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxUtilities.java,v 1.9 1999/12/13 03:40:30 sp Exp $
 */
public class SyntaxUtilities {

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * string.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The string to match
     * @return a boolean
     */
    public static boolean regionMatches(final boolean ignoreCase, final Segment text,
                                        final int offset, final String match) {
        final int length = offset + match.length();
        final char[] textArray = text.array;
        if (length > text.offset + text.count)
            return false;
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match.charAt(j);
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2)
                return false;
        }
        return true;
    }

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * character array.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The character array to match
     * @return a boolean
     */
    public static boolean regionMatches(final boolean ignoreCase, final Segment text,
                                        final int offset, final char[] match) {
        final int length = offset + match.length;
        final char[] textArray = text.array;
        if (length > text.offset + text.count)
            return false;
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match[j];
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2)
                return false;
        }
        return true;
    }

    /**
     * Returns the default style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the default syntax styles.
     *
     * @return an array of {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle} objects
     */
    public static SyntaxStyle[] getDefaultSyntaxStyles() {
        final SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        // SyntaxStyle constructor params: color, italic?, bold?
        // All need to be assigned even if not used by language (no gaps in array)
        styles[Token.NULL] = new SyntaxStyle(Color.black, false, false);
        styles[Token.COMMENT1] = new SyntaxStyle(new Color(0x00CC33), true, false);// (Color.black,true,false);
        styles[Token.COMMENT2] = new SyntaxStyle(new Color(0x990033), true, false);
        styles[Token.KEYWORD1] = new SyntaxStyle(Color.blue, false, false);// (Color.black,false,true);
        styles[Token.KEYWORD2] = new SyntaxStyle(Color.magenta, false, false);
        styles[Token.KEYWORD3] = new SyntaxStyle(Color.red, false, false);// (new Color(0x009600),false,false);
        styles[Token.LITERAL1] = new SyntaxStyle(new Color(0x00CC33), false, false);// (new
        // Color(0x650099),false,false);
        styles[Token.LITERAL2] = new SyntaxStyle(new Color(0x00CC33), false, false);// (new Color(0x650099),false,true);
        styles[Token.LABEL] = new SyntaxStyle(Color.black, true, false);// (new Color(0x990033),false,true);
        styles[Token.OPERATOR] = new SyntaxStyle(Color.black, false, true);
        styles[Token.INVALID] = new SyntaxStyle(Color.red, false, false);
        styles[Token.MACRO_ARG] = new SyntaxStyle(new Color(150, 150, 0), false, false);
        return styles;
    }

    /**
     * Returns the CURRENT style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the current syntax styles. If changes have been made
     * via MARS Settings menu, the current settings will not be the
     * same as the default settings.
     *
     * @return an array of {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.SyntaxStyle} objects
     */
    public static SyntaxStyle[] getCurrentSyntaxStyles() {
        final SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        styles[Token.NULL] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.NULL);
        styles[Token.COMMENT1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.COMMENT1);
        styles[Token.COMMENT2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.COMMENT2);
        styles[Token.KEYWORD1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD1);
        styles[Token.KEYWORD2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD2);
        styles[Token.KEYWORD3] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD3);
        styles[Token.LITERAL1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LITERAL1);
        styles[Token.LITERAL2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LITERAL2);
        styles[Token.LABEL] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LABEL);
        styles[Token.OPERATOR] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.OPERATOR);
        styles[Token.INVALID] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.INVALID);
        styles[Token.MACRO_ARG] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.MACRO_ARG);
        return styles;
    }

    /**
     * Constant <code>popupShowing=false</code>
     */
    public static final boolean popupShowing = false;
    /**
     * Constant <code>popup</code>
     */
    public static Popup popup;

    /**
     * Paints the specified line onto the graphics context. Note that this
     * method munges the offset and count values of the segment.
     *
     * @param line     The line segment
     * @param tokens   The token list for the line
     * @param styles   The syntax style list
     * @param expander The tab expander used to determine tab stops. May
     *                 be null
     * @param gfx      The graphics context
     * @param x        The x co-ordinate
     * @param y        The y co-ordinate
     * @return The x co-ordinate, plus the width of the painted string
     */
    public static int paintSyntaxLine(final Segment line, Token tokens,
                                      final SyntaxStyle[] styles, final TabExpander expander, final Graphics gfx,
                                      int x, final int y) {
        final Font defaultFont = gfx.getFont();
        final Color defaultColor = gfx.getColor();

        int offset = 0;
        for (; ; ) {
            final byte id = tokens.id;
            if (id == Token.END)
                break;

            final int length = tokens.length;
            if (id == Token.NULL) {
                if (!defaultColor.equals(gfx.getColor()))
                    gfx.setColor(defaultColor);
                if (!defaultFont.equals(gfx.getFont()))
                    gfx.setFont(defaultFont);
            } else
                styles[id].setGraphicsFlags(gfx, defaultFont);
            line.count = length;

            if (id == Token.KEYWORD1) {
                // System.out.println("Instruction: "+line);
                if (!SyntaxUtilities.popupShowing) {// System.out.println("creating popup");
                    // JComponent paintArea = (JComponent) expander;
                    // JToolTip tip = paintArea.createToolTip();
                    // tip.setTipText("Instruction: "+line);
                    // Point screenLocation = paintArea.getLocationOnScreen();
                    // PopupFactory popupFactory = PopupFactory.getSharedInstance();
                    // popup = popupFactory.getPopup(paintArea, tip, screenLocation.x + x,
                    // screenLocation.y + y);
                    // popupShowing = true;
                    // popup.show();
                    // int delay = 200; //milliseconds
                    // ActionListener taskPerformer =
                    // new ActionListener() {
                    // public void actionPerformed(ActionEvent evt) {
                    // //popupShowing = false;
                    // if (popup!= null) {
                    // popup.hide();
                    // }
                    // }
                    // };
                    // Timer popupTimer = new Timer(delay, taskPerformer);
                    // popupTimer.setRepeats(false);
                    // popupTimer.start();

                }

                // ToolTipManager.sharedInstance().mouseMoved(
                // new MouseEvent((Component)expander, MouseEvent.MOUSE_MOVED, new
                // java.util.Date().getTime(), 0, x, y, 0, false));
                // new InstructionMouseEvent((Component)expander, x, y, line));
            }

            x = (int) Utilities.drawTabbedText(line, (float) x, (float) y, (Graphics2D) gfx, expander, 0);
            line.offset += length;
            offset += length;

            tokens = tokens.next;
        }

        return x;
    }

    private SyntaxUtilities() {
    }
}

class InstructionMouseEvent extends MouseEvent {
    // private members
    private static final Logger LOGGER = LogManager.getLogger();
    private final Segment line;

    /**
     * <p>Constructor for InstructionMouseEvent.</p>
     *
     * @param component a {@link java.awt.Component} object
     * @param x         a int
     * @param y         a int
     * @param line      a {@link javax.swing.text.Segment} object
     */
    public InstructionMouseEvent(final Component component, final int x, final int y, final Segment line) {
        super(component, MouseEvent.MOUSE_MOVED, new java.util.Date().getTime(), 0, x, y, 0, false);
        InstructionMouseEvent.LOGGER.debug("Create InstructionMouseEvent " + x + " " + y + " " + line);
        this.line = line;
    }

    /**
     * <p>Getter for the field <code>line</code>.</p>
     *
     * @return a {@link javax.swing.text.Segment} object
     */
    public Segment getLine() {
        return this.line;
    }
}
