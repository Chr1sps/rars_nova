package rars.assembler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Represents the list of tokens in a single line of code. It uses, but is not
 * a subclass of, ArrayList.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class TokenList implements Cloneable, Iterable<Token> {

    private @NotNull ArrayList<Token> tokenList;
    private @NotNull String processedLine;// DPS 03-Jan-2013

    /**
     * Constructor for objects of class TokenList
     */
    public TokenList() {
        this.tokenList = new ArrayList<>();
        this.processedLine = ""; // DPS 03-Jan-2013
    }

    /**
     * <p>iterator.</p>
     *
     * @return a {@link java.util.Iterator} object
     */
    @Override
    public @NotNull Iterator<Token> iterator() {
        return this.tokenList.iterator();
    }

    /**
     * Retrieve the source line String associated with this
     * token list. It may or may not have been modified during
     * assembly preprocessing.
     *
     * @return The source line for this token list.
     */
    // DPS 03-Jan-2013/
    public @NotNull String getProcessedLine() {
        return this.processedLine;
    }

    /**
     * Use this to record the source line String for this token list
     * after possible modification (textual substitution) during
     * assembly preprocessing. The modified source will be displayed in
     * the Text Segment Display.
     *
     * @param line The source line, possibly modified (possibly not)
     */
    // DPS 03-Jan-2013
    public void setProcessedLine(final String line) {
        this.processedLine = line;
    }

    /**
     * Returns requested token given position number (starting at 0).
     *
     * @param pos Position in token list.
     * @return the requested token, or ArrayIndexOutOfBounds exception
     */
    public @NotNull Token get(final int pos) {
        return this.tokenList.get(pos);
    }

    /**
     * Replaces token at position with different one. Will throw
     * ArrayIndexOutOfBounds exception if position does not exist.
     *
     * @param pos         Position in token list.
     * @param replacement Replacement token
     */
    public void set(final int pos, final Token replacement) {
        this.tokenList.set(pos, replacement);
    }

    /**
     * Returns number of tokens in list.
     *
     * @return token count.
     */
    public int size() {
        return this.tokenList.size();
    }

    /**
     * Adds a Token object to the end of the list.
     *
     * @param token Token object to be added.
     */
    public void add(final Token token) {
        this.tokenList.add(token);
    }

    /**
     * Removes Token object at specified list position. Uses ArrayList remove
     * method.
     *
     * @param pos Position in token list. Subsequent Tokens are shifted one position
     *            left.
     * @throws java.lang.IndexOutOfBoundsException if <code>pos</code> is &lt; 0 or &ge;
     *                                             <code>size()</code>
     */
    public void remove(final int pos) {
        this.tokenList.remove(pos);
    }

    /**
     * Returns empty/non-empty status of list.
     *
     * @return <code>true</code> if list has no tokens, else <code>false</code>.
     */
    public boolean isEmpty() {
        return this.tokenList.isEmpty();
    }

    /**
     * Get a String representing the token list.
     *
     * @return String version of the token list
     * (a blank is inserted after each token).
     */
    @Override
    public String toString() {
        final StringBuilder stringified = new StringBuilder();
        for (final Token token : this.tokenList) {
            stringified.append(token.toString()).append(" ");
        }
        return stringified.toString();
    }

    /**
     * Get a String representing the sequence of token types for this list.
     *
     * @return String version of the token types for this list
     * (a blank is inserted after each token type).
     */
    public String toTypeString() {
        final StringBuilder stringified = new StringBuilder();
        for (final Token t : this.tokenList) {
            stringified.append(t.getType().toString()).append(" ");
        }
        return stringified.toString();
    }

    /**
     * Makes clone (shallow copy) of this token list object.
     *
     * @return the cloned list.
     */
    // Clones are a bit tricky. super.clone() handles primitives (e.g. values)
    // correctly
    // but the ArrayList itself has to be cloned separately -- otherwise clone will
    // have
    // alias to original token list!!
    @Override
    public Object clone() {
        try {
            final TokenList t = (TokenList) super.clone();
            t.tokenList = new ArrayList<>(this.tokenList);
            return t;
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }
}