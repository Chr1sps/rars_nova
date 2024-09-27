/*
 * TokenMarker.java - Generic token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax.tokenmarker;

import org.jetbrains.annotations.Nullable;
import rars.venus.editors.jeditsyntax.PopupHelpItem;

import javax.swing.text.Segment;
import java.util.ArrayList;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.
 * <p>
 * <p>
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov
 * @version $Id: TokenMarker.java,v 1.32 1999/12/13 03:40:30 sp Exp $
 */
public abstract class TokenMarker {
    /**
     * The first token in the list. This should be used as the return
     * value from <code>markTokens()</code>.
     */
    protected Token firstToken;
    /**
     * The last token in the list. New tokens are added here.
     * This should be set to null before a new line is to be tokenized.
     */
    protected Token lastToken;
    /**
     * An array for storing information about lines. It is enlarged and
     * shrunk automatically by the <code>insertLines()</code> and
     * <code>deleteLines()</code> methods.
     */
    protected LineInfo[] lineInfo;
    /**
     * The number of lines in the model being tokenized. This can be
     * less than the length of the <code>lineInfo</code> array.
     */
    protected int length;
    /**
     * The last tokenized line.
     */
    protected int lastLine;
    /**
     * True if the next line should be painted.
     */
    protected boolean nextLineRequested;

    /**
     * Creates a new <code>TokenMarker</code>. This DOES NOT create
     * a lineInfo array; an initial call to <code>insertLines()</code>
     * does that.
     */
    protected TokenMarker() {
        this.lastLine = -1;
    }

    /**
     * Returns if the token marker supports tokens that span multiple
     * lines. If this is true, the object using this token marker is
     * required to pass all lines in the document to the
     * <code>markTokens()</code> method (in turn).
     * <p>
     * <p>
     * The default implementation returns true; it should be overridden
     * to return false on simpler token markers for increased speed.
     *
     * @return a boolean
     */
    public static boolean supportsMultilineTokens() {
        return true;
    }

    /**
     * A wrapper for the lower-level <code>markTokensImpl</code> method
     * that is called to split a line up into tokens.
     *
     * @param line      The line
     * @param lineIndex The line number
     * @return a {@link Token} object
     */
    public Token markTokens(final Segment line, final int lineIndex) {
        if (lineIndex >= this.length) {
            throw new IllegalArgumentException("Tokenizing invalid line: "
                    + lineIndex);
        }

        this.lastToken = null;

        final LineInfo info = this.lineInfo[lineIndex];
        final LineInfo prev;
        if (lineIndex == 0)
            prev = null;
        else
            prev = this.lineInfo[lineIndex - 1];

        final byte oldToken = info.token;
        final byte token = this.markTokensImpl(prev == null ? Token.NULL : prev.token, line, lineIndex);

        info.token = token;

        /*
         * This is a foul hack. It stops nextLineRequested from being cleared if
         * the same line is marked twice.
         *
         * Why is this necessary? It's all JEditTextArea's fault. When something
         * is inserted into the text, firing a document event, the
         * insertUpdate() method shifts the caret (if necessary) by the amount
         * inserted.
         *
         * All caret movement is handled by the select() method, which
         * eventually pipes the new position to scrollTo() and calls repaint().
         *
         * Note that at this point in time, the new line hasn't yet been
         * painted; the caret is moved first.
         *
         * scrollTo() calls offsetToX(), which tokenizes the line unless it is
         * being called on the last line painted (in which case it uses the text
         * area's painter cached token list). What scrollTo() does next is
         * irrelevant.
         *
         * After scrollTo() has done it's job, repaint() is called, and
         * eventually we end up in paintLine(), whose job is to paint the
         * changed line. It, too, calls markTokens().
         *
         * The problem was that if the line started a multiline token, the first
         * markTokens() (done in offsetToX()) would set nextLineRequested
         * (because the line end token had changed) but the second would clear
         * it (because the line was the same that time) and therefore
         * paintLine() would never know that it needed to repaint subsequent
         * lines.
         *
         * This bug took me ages to track down, that's why I wrote all the
         * relevant info down so that others wouldn't duplicate it.
         */
        if (!(this.lastLine == lineIndex && this.nextLineRequested))
            this.nextLineRequested = (oldToken != token);

        this.lastLine = lineIndex;

        this.addToken(0, Token.END);

        return this.firstToken;
    }

    // protected members

    /**
     * An abstract method that splits a line up into tokens. It
     * should parse the line, and call <code>addToken()</code> to
     * add syntax tokens to the token list. Then, it should return
     * the initial token type for the next line.
     * <p>
     * <p>
     * For example if the current line contains the start of a
     * multiline comment that doesn't end on that line, this method
     * should return the comment token type so that it continues on
     * the next line.
     *
     * @param token     The initial token type for this line
     * @param line      The line to be tokenized
     * @param lineIndex The index of the line in the document, starting at 0
     * @return The initial token type for the next line
     */
    protected abstract byte markTokensImpl(byte token, Segment line,
                                           int lineIndex);

    /**
     * Informs the token marker that lines have been inserted into
     * the document. This inserts a gap in the <code>lineInfo</code>
     * array.
     *
     * @param index The first line number
     * @param lines The number of lines
     */
    public void insertLines(final int index, final int lines) {
        if (lines <= 0)
            return;
        this.length += lines;
        this.ensureCapacity(this.length);
        final int len = index + lines;
        System.arraycopy(this.lineInfo, index, this.lineInfo, len, this.lineInfo.length - len);

        for (int i = index + lines - 1; i >= index; i--) {
            this.lineInfo[i] = new LineInfo();
        }
    }

    /**
     * Informs the token marker that line have been deleted from
     * the document. This removes the lines in question from the
     * <code>lineInfo</code> array.
     *
     * @param index The first line number
     * @param lines The number of lines
     */
    public void deleteLines(final int index, final int lines) {
        if (lines <= 0)
            return;
        final int len = index + lines;
        this.length -= lines;
        System.arraycopy(this.lineInfo, len, this.lineInfo,
                index, this.lineInfo.length - len);
    }

    /**
     * Returns the number of lines in this token marker.
     *
     * @return a int
     */
    public int getLineCount() {
        return this.length;
    }

    /**
     * Returns true if the next line should be repainted. This
     * will return true after a line has been tokenized that starts
     * a multiline token that continues onto the next line.
     *
     * @return a boolean
     */
    public boolean isNextLineRequested() {
        return this.nextLineRequested;
    }

    /**
     * Construct and return any appropriate help information for
     * the given token. This default definition returns null;
     * override it in language-specific subclasses.
     *
     * @param token     the pertinent Token object
     * @param tokenText the source String that matched to the token
     * @return ArrayList containing PopupHelpItem objects, one per match.
     */
    public @Nullable ArrayList<PopupHelpItem> getTokenExactMatchHelp(final Token token, final String tokenText) {
        return null;
    }

    /**
     * Construct and return any appropriate help information for
     * the given token or "token prefix". Will match instruction prefixes, e.g. "s"
     * matches "sw".
     * This default definition returns null;
     * override it in language-specific subclasses.
     *
     * @param line          String containing current line
     * @param tokenList     first Token on the current line
     * @param tokenAtOffset the pertinent Token object
     * @param tokenText     the source String that matched to the token
     * @return ArrayList containing PopupHelpItem objects, one per match.
     */
    public @Nullable ArrayList<PopupHelpItem> getTokenPrefixMatchHelp(final String line, final Token tokenList, final Token tokenAtOffset,
                                                                      final String tokenText) {
        return null;
    }

    /**
     * Ensures that the <code>lineInfo</code> array can contain the
     * specified index. This enlarges it if necessary. No action is
     * taken if the array is large enough already.
     * <p>
     * <p>
     * It should be unnecessary to call this under normal
     * circumstances; <code>insertLine()</code> should take care of
     * enlarging the line info array automatically.
     *
     * @param index The array index
     */
    protected void ensureCapacity(final int index) {
        if (this.lineInfo == null)
            this.lineInfo = new LineInfo[index + 1];
        else if (this.lineInfo.length <= index) {
            final LineInfo[] lineInfoN = new LineInfo[(index + 1) * 2];
            System.arraycopy(this.lineInfo, 0, lineInfoN, 0,
                    this.lineInfo.length);
            this.lineInfo = lineInfoN;
        }
    }

    /**
     * Adds a token to the token list.
     *
     * @param length The length of the token
     * @param id     The id of the token
     */
    protected void addToken(final int length, final byte id) {
        if (id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
            throw new InternalError("Invalid id: " + id);

        if (length == 0 && id != Token.END)
            return;

        if (this.firstToken == null) {
            this.firstToken = new Token(length, id);
            this.lastToken = this.firstToken;
        } else if (this.lastToken == null) {
            this.lastToken = this.firstToken;
            this.firstToken.length = length;
            this.firstToken.id = id;
        } else if (this.lastToken.next == null) {
            this.lastToken.next = new Token(length, id);
            this.lastToken = this.lastToken.next;
        } else {
            this.lastToken = this.lastToken.next;
            this.lastToken.length = length;
            this.lastToken.id = id;
        }
    }

    /**
     * Inner class for storing information about tokenized lines.
     */
    public static class LineInfo {
        /**
         * The id of the last token of the line.
         */
        public byte token;
        /**
         * This is for use by the token marker implementations
         * themselves. It can be used to store anything that
         * is an object and that needs to exist on a per-line
         * basis.
         */
        public @Nullable Object obj;

        /**
         * Creates a new LineInfo object with token = Token.NULL
         * and obj = null.
         */
        public LineInfo() {
        }

        /**
         * Creates a new LineInfo object with the specified
         * parameters.
         */
        public LineInfo(final byte token, final Object obj) {
            this.token = token;
            this.obj = obj;
        }
    }
}
