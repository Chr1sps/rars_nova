/*
 * TextUtilities.java - Utility functions used by the text area classes
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Class with several utility functions used by the text area component.
 *
 * @author Slava Pestov
 * @version $Id: TextUtilities.java,v 1.4 1999/12/13 03:40:30 sp Exp $
 */
public final class TextUtilities {
    private TextUtilities() {
    }

    /**
     * Returns the offset of the bracket matching the one at the
     * specified offset of the document, or -1 if the bracket is
     * unmatched (or if the character is not a bracket).
     *
     * @param doc    The document
     * @param offset The offset
     * @return a int
     * @throws javax.swing.text.BadLocationException If an out-of-bounds access
     *                                               was attempted on the document text
     */
    public static int findMatchingBracket(final Document doc, int offset)
            throws BadLocationException {
        if (doc.getLength() == 0)
            return -1;
        final char c = doc.getText(offset, 1).charAt(0);
        final char cprime; // c` - corresponding character
        final boolean direction; // true = back, false = forward

        switch (c) {
            case '(':
                cprime = ')';
                direction = false;
                break;
            case ')':
                cprime = '(';
                direction = true;
                break;
            case '[':
                cprime = ']';
                direction = false;
                break;
            case ']':
                cprime = '[';
                direction = true;
                break;
            case '{':
                cprime = '}';
                direction = false;
                break;
            case '}':
                cprime = '{';
                direction = true;
                break;
            default:
                return -1;
        }

        int count;

        // How to merge these two cases is left as an exercise
        // for the reader.

        // Go back or forward
        if (direction) {
            // Count is 1 initially because we have already
            // `found' one closing bracket
            count = 1;

            // Get text[0,offset-1];
            final String text = doc.getText(0, offset);

            // Scan backwards
            for (int i = offset - 1; i >= 0; i--) {
                // If text[i] == c, we have found another
                // closing bracket, therefore we will need
                // two opening brackets to complete the
                // match.
                final char x = text.charAt(i);
                if (x == c)
                    count++;

                    // If text[i] == cprime, we have found a
                    // opening bracket, so we return i if
                    // --count == 0
                else if (x == cprime) {
                    if (--count == 0)
                        return i;
                }
            }
        } else {
            // Count is 1 initially because we have already
            // `found' one opening bracket
            count = 1;

            // So we don't have to + 1 in every loop
            offset++;

            // Number of characters to check
            final int len = doc.getLength() - offset;

            // Get text[offset+1,len];
            final String text = doc.getText(offset, len);

            // Scan forwards
            for (int i = 0; i < len; i++) {
                // If text[i] == c, we have found another
                // opening bracket, therefore we will need
                // two closing brackets to complete the
                // match.
                final char x = text.charAt(i);

                if (x == c)
                    count++;

                    // If text[i] == cprime, we have found an
                    // closing bracket, so we return i if
                    // --count == 0
                else if (x == cprime) {
                    if (--count == 0)
                        return i + offset;
                }
            }
        }

        // Nothing found
        return -1;
    }

    /**
     * Locates the start of the word at the specified position.
     *
     * @param line      The text
     * @param pos       The position
     * @param noWordSep a {@link java.lang.String} object
     * @return a int
     */
    public static int findWordStart(final String line, final int pos, String noWordSep) {
        char ch = line.charAt(pos - 1);

        if (noWordSep == null)
            noWordSep = "";
        final boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                && noWordSep.indexOf(ch) == -1);

        int wordStart = 0;
        for (int i = pos - 1; i >= 0; i--) {
            ch = line.charAt(i);
            if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                    noWordSep.indexOf(ch) == -1)) {
                wordStart = i + 1;
                break;
            }
        }

        return wordStart;
    }

    /**
     * Locates the end of the word at the specified position.
     *
     * @param line      The text
     * @param pos       The position
     * @param noWordSep a {@link java.lang.String} object
     * @return a int
     */
    public static int findWordEnd(final String line, final int pos, String noWordSep) {
        char ch = line.charAt(pos);

        if (noWordSep == null)
            noWordSep = "";
        final boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                && noWordSep.indexOf(ch) == -1);

        int wordEnd = line.length();
        for (int i = pos; i < line.length(); i++) {
            ch = line.charAt(i);
            if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                    noWordSep.indexOf(ch) == -1)) {
                wordEnd = i;
                break;
            }
        }
        return wordEnd;
    }

    /**
     * Prefix all lines with the specified prefix.
     *
     * @param text   The text
     * @param prefix The prefix
     * @return a {@link java.lang.String} object
     */
    public static String addLinePrefixes(final String text, final String prefix) {
        final String[] lines = text.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            lines[i] = prefix + lines[i];
        }

        return String.join("\n", lines);
    }

    /**
     * Delete all lines of the specified prefix.
     *
     * @param text   The text
     * @param prefix The prefix
     * @return a {@link java.lang.String} object
     */
    public static String deleteLinePrefixes(final String text, final String prefix) {
        final String[] lines = text.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replaceFirst(prefix, "");
        }

        return String.join("\n", lines);
    }
}