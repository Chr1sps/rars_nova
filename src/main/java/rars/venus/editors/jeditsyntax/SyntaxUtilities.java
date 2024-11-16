/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;
import rars.venus.editors.jeditsyntax.tokenmarker.Token;
import rars.venus.editors.jeditsyntax.tokenmarker.TokenType;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Class with several utility functions used by jEdit's syntax colorizing
 * subsystem.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxUtilities.java,v 1.9 1999/12/13 03:40:30 sp Exp $
 */
public final class SyntaxUtilities {

    private static final @Unmodifiable Map<TokenType, SyntaxStyle> defaultStyles = Map.ofEntries(
            entry(TokenType.NULL, new SyntaxStyle(Color.black, false, false)),
            entry(TokenType.COMMENT1, new SyntaxStyle(new Color(0x00CC33), true, false)),
            entry(TokenType.COMMENT2, new SyntaxStyle(new Color(0x990033), true, false)),
            entry(TokenType.KEYWORD1, new SyntaxStyle(Color.blue, false, false)),
            entry(TokenType.KEYWORD2, new SyntaxStyle(Color.magenta, false, false)),
            entry(TokenType.KEYWORD3, new SyntaxStyle(Color.red, false, false)),
            entry(TokenType.LITERAL1, new SyntaxStyle(new Color(0x00CC33), false, false)),
            entry(TokenType.LITERAL2, new SyntaxStyle(new Color(0x00CC33), false, false)),
            entry(TokenType.LABEL, new SyntaxStyle(Color.black, true, false)),
            entry(TokenType.OPERATOR, new SyntaxStyle(Color.black, false, true)),
            entry(TokenType.INVALID, new SyntaxStyle(Color.red, false, false)),
            entry(TokenType.MACRO_ARG, new SyntaxStyle(new Color(150, 150, 0), false, false)
            ));

    private SyntaxUtilities() {
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
    public static boolean regionMatches(final boolean ignoreCase, final @NotNull Segment text,
                                        final int offset, final char @NotNull [] match) {
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
     * @return a map of {@link SyntaxStyle} objects corresponding to each {@link TokenType}
     */
    public static @NotNull @Unmodifiable Map<TokenType, SyntaxStyle> getDefaultSyntaxStyles() {
        return defaultStyles;
    }

    /**
     * Returns the CURRENT style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the current syntax styles. If changes have been made
     * via MARS Settings menu, the current settings will not be the
     * same as the default settings.
     *
     * @return a map of {@link SyntaxStyle} objects corresponding to each {@link TokenType}
     */
    public static @NotNull @Unmodifiable Map<TokenType, SyntaxStyle> getCurrentSyntaxStyles() {
        return Map.ofEntries(
                entry(TokenType.NULL, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.NULL)),
                entry(TokenType.COMMENT1, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.COMMENT1)),
                entry(TokenType.COMMENT2, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.COMMENT2)),
                entry(TokenType.KEYWORD1, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.KEYWORD1)),
                entry(TokenType.KEYWORD2, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.KEYWORD2)),
                entry(TokenType.KEYWORD3, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.KEYWORD3)),
                entry(TokenType.LITERAL1, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.LITERAL1)),
                entry(TokenType.LITERAL2, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.LITERAL2)),
                entry(TokenType.LABEL, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.LABEL)),
                entry(TokenType.OPERATOR, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.OPERATOR)),
                entry(TokenType.INVALID, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.INVALID)),
                entry(TokenType.MACRO_ARG, Globals.getSettings().getEditorSyntaxStyleByTokenType(TokenType.MACRO_ARG))
        );
    }

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
    public static int paintSyntaxLine(final Segment line, final @NotNull List<Token> tokens,
                                      final @NotNull Map<TokenType, SyntaxStyle> styles, final TabExpander expander, final @NotNull Graphics gfx,
                                      int x, final int y) {
        final Font defaultFont = gfx.getFont();
        final Color defaultColor = gfx.getColor();

        int offset = 0;
        for (final var token : tokens) {
            final var type = token.type();
            final int length = token.length();
            if (type == TokenType.END)
                break;

            if (type == TokenType.NULL) {
                if (!defaultColor.equals(gfx.getColor()))
                    gfx.setColor(defaultColor);
                if (!defaultFont.equals(gfx.getFont()))
                    gfx.setFont(defaultFont);
            } else
                styles.get(type).setGraphicsFlags(gfx, defaultFont);
            line.count = length;

            x = (int) Utilities.drawTabbedText(line, (float) x, (float) y, (Graphics2D) gfx, expander, 0);
            line.offset += length;
            offset += length;
        }
        return x;
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
        InstructionMouseEvent.LOGGER.debug("Create InstructionMouseEvent {} {} {}", x, y, line);
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
