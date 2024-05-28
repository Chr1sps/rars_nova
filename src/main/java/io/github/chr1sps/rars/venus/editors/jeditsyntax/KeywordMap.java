/*
 * KeywordMap.java - Fast keyword->id map
 * Copyright (C) 1998, 1999 Slava Pestov
 * Copyright (C) 1999 Mike Dillon
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package io.github.chr1sps.rars.venus.editors.jeditsyntax;

import io.github.chr1sps.rars.venus.editors.jeditsyntax.tokenmarker.Token;

import javax.swing.text.Segment;

/**
 * A <code>KeywordMap</code> is similar to a hashtable in that it maps keys
 * to values. However, the `keys' are Swing segments. This allows lookups of
 * text substrings without the overhead of creating a new string object.
 * <p>
 * This class is used by <code>CTokenMarker</code> to map keywords to ids.
 *
 * @author Slava Pestov, Mike Dillon
 * @version $Id: KeywordMap.java,v 1.16 1999/12/13 03:40:30 sp Exp $
 */
public class KeywordMap {
    /**
     * Creates a new <code>KeywordMap</code>.
     *
     * @param ignoreCase True if keys are case insensitive
     */
    public KeywordMap(final boolean ignoreCase) {
        this(ignoreCase, 52);
        this.ignoreCase = ignoreCase;
    }

    /**
     * Creates a new <code>KeywordMap</code>.
     *
     * @param ignoreCase True if the keys are case insensitive
     * @param mapLength  The number of `buckets' to create.
     *                   A value of 52 will give good performance for most maps.
     */
    public KeywordMap(final boolean ignoreCase, final int mapLength) {
        this.mapLength = mapLength;
        this.ignoreCase = ignoreCase;
        this.map = new Keyword[mapLength];
    }

    /**
     * Looks up a key.
     *
     * @param text   The text segment
     * @param offset The offset of the substring within the text segment
     * @param length The length of the substring
     * @return a byte
     */
    public byte lookup(final Segment text, final int offset, final int length) {
        if (length == 0)
            return Token.NULL;
        if (text.array[offset] == '%')
            return Token.MACRO_ARG; // added 12/12 M. Sekhavat
        Keyword k = this.map[this.getSegmentMapKey(text, offset, length)];
        while (k != null) {
            if (length != k.keyword.length) {
                k = k.next;
                continue;
            }
            if (SyntaxUtilities.regionMatches(this.ignoreCase, text, offset,
                    k.keyword))
                return k.id;
            k = k.next;
        }
        return Token.NULL;
    }

    /**
     * Adds a key-value mapping.
     *
     * @param keyword The key
     * @param id      The value
     */
    public void add(final String keyword, final byte id) {
        final int key = this.getStringMapKey(keyword);
        this.map[key] = new Keyword(keyword.toCharArray(), id, this.map[key]);
    }

    /**
     * Returns true if the keyword map is set to be case insensitive,
     * false otherwise.
     *
     * @return a boolean
     */
    public boolean getIgnoreCase() {
        return this.ignoreCase;
    }

    /**
     * Sets if the keyword map should be case insensitive.
     *
     * @param ignoreCase True if the keyword map should be case
     *                   insensitive, false otherwise
     */
    public void setIgnoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    // protected members
    protected final int mapLength;

    /**
     * <p>getStringMapKey.</p>
     *
     * @param s a {@link java.lang.String} object
     * @return a int
     */
    protected int getStringMapKey(final String s) {
        return (Character.toUpperCase(s.charAt(0)) +
                Character.toUpperCase(s.charAt(s.length() - 1)))
                % this.mapLength;
    }

    /**
     * <p>getSegmentMapKey.</p>
     *
     * @param s   a {@link javax.swing.text.Segment} object
     * @param off a int
     * @param len a int
     * @return a int
     */
    protected int getSegmentMapKey(final Segment s, final int off, final int len) {
        return (Character.toUpperCase(s.array[off]) +
                Character.toUpperCase(s.array[off + len - 1]))
                % this.mapLength;
    }

    // private members
    class Keyword {
        public Keyword(final char[] keyword, final byte id, final Keyword next) {
            this.keyword = keyword;
            this.id = id;
            this.next = next;
        }

        public final char[] keyword;
        public final byte id;
        public final Keyword next;
    }

    private final Keyword[] map;
    private boolean ignoreCase;
}
