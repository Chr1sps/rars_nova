package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.RISCVProgram;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Represents one token in the input program. Each Token carries, along with its
 * type and value, the position (line, column) in which its source appears in
 * the program.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class Token {

    private final @NotNull String value;
    private final @Nullable RISCVProgram sourceProgram;
    private final int sourceLine;
    private final int sourcePos;
    private @NotNull TokenType type;
    // original program and line will differ from the above if token was defined in
    // an included file
    private int originalSourceLine;

    /**
     * Constructor for Token class.
     *
     * @param type
     *     The token type that this token has. (e.g. REGISTER_NAME)
     * @param value
     *     The source value for this token (e.g. $t3)
     * @param sourceProgram
     *     The RISCVprogram object containing this token
     * @param line
     *     The line number in source program in which this token
     *     appears.
     * @param start
     *     The starting position in that line number of this
     *     token's source value.
     * @see TokenType
     */
    public Token(
        final @NotNull TokenType type,
        final @NotNull String value,
        final @Nullable RISCVProgram sourceProgram,
        final int line,
        final int start
    ) {
        this.type = type;
        this.value = value;
        this.sourceProgram = sourceProgram;
        this.sourceLine = line;
        this.sourcePos = start;
        this.originalSourceLine = line;
    }

    /**
     * Set original program and line number for this token.
     * Line number or both may change during pre-assembly as a result
     * of the ".include" directive, and we need to keep the original
     * for later reference (error messages, text segment display).
     *
     * @param origSourceLine
     *     Line within that program of this token.
     */
    public void setOriginal(final int origSourceLine) {
        this.originalSourceLine = origSourceLine;
    }

    /**
     * Produces original line number of this token. It could change as result
     * of ".include"
     *
     * @return original line number of this token.
     */
    public int getOriginalSourceLine() {
        return this.originalSourceLine;
    }

    /**
     * Produces token type of this token.
     *
     * @return TokenType of this token.
     */
    public @NotNull TokenType getType() {
        return this.type;
    }

    /**
     * Set or modify token type. Generally used to note that
     * an identifier that matches an instruction name is
     * actually being used as a label.
     *
     * @param type
     *     new TokenTypes for this token.
     */
    public void setType(final @NotNull TokenType type) {
        // TODO: remove this method
        // It is *highly* preferable to make this class immutable and, thus,
        // make any token manipulation more predictable by avoiding side effects
        // created via mutation and references.
        this.type = type;
    }

    /**
     * Produces source code of this token.
     *
     * @return String containing source code of this token.
     */
    public @NotNull String getText() {
        return this.value;
    }

    /**
     * Get a String representing the token. This method is
     * equivalent to getValue().
     *
     * @return String version of the token.
     */
    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Produces RISCVprogram object associated with this token.
     *
     * @return RISCVprogram object associated with this token.
     */
    public @Nullable RISCVProgram getSourceProgram() {
        return this.sourceProgram;
    }

    /**
     * Produces line number of source program of this token.
     *
     * @return line number in source program of this token.
     */
    public int getSourceLine() {
        return this.sourceLine;
    }

    /**
     * Produces position within source line of this token.
     *
     * @return first character position within source program line of this token.
     */
    public int getStartPos() {
        return this.sourcePos;
    }

}
