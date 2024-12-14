/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;
import rars.venus.editors.jeditsyntax.tokenmarker.TokenType;

import java.awt.*;
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
}