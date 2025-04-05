package rars.assembler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents the list of tokens in a single line of code. It uses, but is not
 * a subclass of, ArrayList.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class TokenList implements Cloneable, Collection<@NotNull Token> {

    private @NotNull ArrayList<@NotNull Token> tokenList;
    private @NotNull String processedLine;

    /**
     * Constructor for objects of class TokenList
     */
    public TokenList() {
        this.tokenList = new ArrayList<>();
        this.processedLine = "";
    }

    @Override
    public @NotNull Iterator<Token> iterator() {
        return this.tokenList.iterator();
    }

    @Override
    public @NotNull Object @NotNull [] toArray() {
        return this.tokenList.toArray();
    }

    @Override
    public @NotNull <T> T @NotNull [] toArray(final @NotNull T @NotNull [] a) {
        return this.tokenList.toArray(a);
    }

    /**
     * Retrieve the source line String associated with this
     * token list. It may or may not have been modified during
     * assembly preprocessing.
     *
     * @return The source line for this token list.
     */
    public @NotNull String getProcessedLine() {
        return this.processedLine;
    }

    /**
     * Use this to record the source line String for this token list
     * after possible modification (textual substitution) during
     * assembly preprocessing. The modified source will be displayed in
     * the Text Segment Display.
     *
     * @param line
     *     The source line, possibly modified (possibly not)
     */
    public void setProcessedLine(final @NotNull String line) {
        this.processedLine = line;
    }

    /**
     * Returns requested token given position number (starting at 0).
     *
     * @param pos
     *     Position in token list.
     * @return the requested token, or ArrayIndexOutOfBounds exception
     */
    public @NotNull Token get(final int pos) {
        return this.tokenList.get(pos);
    }

    /**
     * Replaces token at position with different one. Will throw
     * ArrayIndexOutOfBounds exception if position does not exist.
     *
     * @param pos
     *     Position in token list.
     * @param replacement
     *     Replacement token
     */
    public void set(final int pos, final Token replacement) {
        this.tokenList.set(pos, replacement);
    }

    /**
     * Returns number of tokens in list.
     *
     * @return token count.
     */
    @Override
    public int size() {
        return this.tokenList.size();
    }

    /**
     * Adds a Token object to the end of the list.
     *
     * @param token
     *     Token object to be added.
     */
    @Override
    public boolean add(final Token token) {
        return this.tokenList.add(token);
    }

    @Override
    public boolean remove(final Object o) {
        return this.tokenList.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NotNull final Collection<? extends Token> c) {
        return this.tokenList.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
        return this.tokenList.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
        return this.tokenList.retainAll(c);
    }

    @Override
    public void clear() {
        this.tokenList.clear();
    }

    /**
     * Removes Token object at specified list position. Uses ArrayList remove
     * method.
     *
     * @param pos
     *     Position in token list. Subsequent Tokens are shifted one position
     *     left.
     * @throws java.lang.IndexOutOfBoundsException
     *     if {@code pos} is &lt; 0 or &ge;
     *     {@code size()}
     */
    public void remove(final int pos) {
        this.tokenList.remove(pos);
    }

    /**
     * Returns empty/non-empty status of list.
     *
     * @return {@code true} if list has no tokens, else {@code false}.
     */
    @Override
    public boolean isEmpty() {
        return this.tokenList.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return this.tokenList.contains(o);
    }

    /**
     * Get a String representing the token list.
     *
     * @return String version of the token list
     * (a blank is inserted after each token).
     */
    @Override
    public String toString() {
        final var builder = new StringBuilder();
        for (final var token : this.tokenList) {
            builder.append(token).append(' ');
        }
        return builder.toString();
    }

    /**
     * Makes clone (shallow copy) of this token list object.
     *
     * @return the cloned list.
     */
    @Override
    public Object clone() {
        // Clones are a bit tricky. super.clone() handles primitives (e.g. values)
        // correctly
        // but the ArrayList itself has to be cloned separately -- otherwise clone will
        // have
        // alias to original token list!!
        try {
            final TokenList t = (TokenList) super.clone();
            t.tokenList = new ArrayList<>(this.tokenList);
            return t;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException("Cloning failed for class TokenList.");
        }
    }
}
