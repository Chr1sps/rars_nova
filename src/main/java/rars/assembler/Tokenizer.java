package rars.assembler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.ErrorMessage;
import rars.Globals;
import rars.RISCVProgram;
import rars.exceptions.AssemblyException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * A tokenizer is capable of tokenizing a complete source program, or a given
 * line from
 * a source program. Since RARS assembly is line-oriented, each line defines a
 * complete statement.
 * Tokenizing is the process of analyzing the input program for the purpose of
 * recognizing each RISCV language element. The types of language elements are
 * known as "tokens".
 * Tokens are defined in the TokenTypes class.<br>
 * <br>
 * Example: <br>
 * The RISCV statement
 * <code>here:  lw  t3, 8(t4)   #load third member of array</code><br>
 * generates the following token list<br>
 * IDENTIFIER, COLON, OPERATOR, REGISTER_NAME, COMMA, INTEGER_5, LEFT_PAREN,
 * REGISTER_NAME, RIGHT_PAREN, COMMENT<br>
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class Tokenizer {
    private static final Logger LOGGER = LogManager.getLogger(Tokenizer.class);
    // The 8 escaped characters are: single quote, double quote, backslash, newline
    // (linefeed),
    // tab, backspace, return, form feed. The characters and their corresponding
    // decimal codes:
    private static final String escapedCharacters = "'\"\\ntbrf0";
    private static final String[] escapedCharactersValues = {"39", "34", "92", "10", "9", "8", "13", "12", "0"};
    private final @NotNull ErrorList errors;
    private final @Nullable RISCVProgram program;
    private final @NotNull HashMap<String, String> equivalents; // DPS 11-July-2012

    private Tokenizer(
        final @Nullable RISCVProgram program,
        final @NotNull ErrorList errorList
    ) {
        this.errors = errorList;
        this.program = program;
        this.equivalents = new HashMap<>();
    }

    private Tokenizer(final @Nullable RISCVProgram program) {
        this(program, new ErrorList());
    }

    private Tokenizer() {
        this(null, new ErrorList());
    }

    /// If passed a candidate character literal, attempt to translate it into integer
    /// constant.
    /// If the translation fails, return original value.
    private static @NotNull String preprocessCharacterLiteral(final @NotNull String value) {
        // must start and end with quote and have something in between
        if (value.length() < 3 || value.charAt(0) != '\'' || value.charAt(value.length() - 1) != '\'') {
            return value;
        }
        final String quotesRemoved = value.substring(1, value.length() - 1);
        // if not escaped, then if one character left return its value else return
        // original.
        if (quotesRemoved.charAt(0) != '\\') {
            return (quotesRemoved.length() == 1) ? Integer.toString(quotesRemoved.charAt(0)) : value;
        }
        // now we know it is escape sequence and have to decode which of the 8:
        // ',",\,n,t,b,r,f
        if (quotesRemoved.length() == 2) {
            final int escapedCharacterIndex = escapedCharacters.indexOf(quotesRemoved.charAt(1));
            return (escapedCharacterIndex >= 0) ? escapedCharactersValues[escapedCharacterIndex] : value;
        }
        // last valid possibility is 3 digit octal code 000 through 377
        if (quotesRemoved.length() == 4) {
            try {
                final int intValue = Integer.parseInt(quotesRemoved.substring(1), 8);
                if (intValue >= 0 && intValue <= 255) {
                    return Integer.toString(intValue);
                }
            } catch (final NumberFormatException ignored) {
            } // if not valid octal, will fall through and reject
        }
        return value;
    }

    /// Will tokenize a complete source program.
    ///
    /// @param program
    ///     The [RISCVProgram] to be tokenized.
    /// @return A [List] representing the tokenized program. Each list member is a [TokenList].
    /// that represents a tokenized source statement from the program.
    /// @throws AssemblyException
    ///     if any.
    public static @NotNull List<TokenList> tokenize(final @NotNull RISCVProgram program) throws AssemblyException {

        final var tokenizer = new Tokenizer(program);
        final var tokenList = new ArrayList<TokenList>();
        final var source = tokenizer.processIncludes(program, new HashMap<>()); // DPS 9-Jan-2013
        program.setSourceLineList(source);

        for (int i = 0; i < source.size(); i++) {
            final var sourceLine = source.get(i).source();
            final var currentLineTokens = tokenizer.tokenizeLineImpl(tokenizer.program, i + 1, sourceLine, true);
            tokenList.add(currentLineTokens);
            // DPS 03-Jan-2013. Related to 11-July-2012. If source code substitution was
            // made
            // based on .eqv directive during tokenizing, the processed line, a String, is
            // not the same object as the original line. Thus I can use != instead of
            // !equals()
            // This IF statement will replace original source with source modified by .eqv
            // substitution.
            // Not needed by assembler, but looks better in the Text Segment Display.
            if (!sourceLine.isEmpty() && !sourceLine.equals(currentLineTokens.getProcessedLine())) {
                source.set(
                    i, new SourceLine(
                        currentLineTokens.getProcessedLine(), source.get(i).program(),
                        source.get(i).lineNumber()
                    )
                );
            }
        }
        if (tokenizer.errors.errorsOccurred()) {
            throw new AssemblyException(tokenizer.errors);
        }
        return tokenList;
    }

    /// Used only to create a token list for the example provided with each
    /// instruction specification.
    ///
    /// @param example
    ///     The example RISCV instruction to be tokenized.
    /// @return A [TokenList] object representing the tokenized instruction.
    /// @throws AssemblyException
    ///     This occurs only if the instruction
    ///                                                                     specification itself contains one or more
    ///                                                                     lexical (i.e. token) errors.
    public static @NotNull TokenList tokenizeExampleInstruction(final @NotNull String example) throws
        AssemblyException {
        final var tokenizer = new Tokenizer();
        final var result = tokenizer.tokenizeLineImpl(null, 0, example, false);
        if (tokenizer.errors.errorsOccurred()) {
            throw new AssemblyException(tokenizer.errors);
        }
        return result;
    }

    /**
     * Will tokenize one line of source code. If lexical errors are discovered,
     * they are noted in an ErrorMessage object which is added to the provided
     * ErrorList
     * instead of the Tokenizer's error list. Will NOT throw an exception.
     *
     * @param lineNum
     *     line number from source code (used in error message)
     * @param theLine
     *     String containing source code
     * @param callerErrorList
     *     errors will go into this list instead of tokenizer's
     *     list.
     * @param doEqvSubstitutes
     *     boolean param set true to perform .eqv substitutions,
     *     else false
     * @return the generated token list for that line
     */
    public static @NotNull TokenList tokenizeLine(
        final int lineNum, final @NotNull String theLine,
        final @NotNull ErrorList callerErrorList,
        final boolean doEqvSubstitutes
    ) {
        final var tokenizer = new Tokenizer(null, callerErrorList);
        return tokenizer.tokenizeLineImpl(null, lineNum, theLine, doEqvSubstitutes);
    }

    // Modified for release 4.3, to preserve existing API.

    /**
     * Will tokenize one line of source code. If lexical errors are discovered,
     * they are noted in an ErrorMessage object which is added to the provided
     * ErrorList
     * instead of the Tokenizer's error list. Will NOT throw an exception.
     *
     * @param program
     *     RISCVprogram containing this line of source
     * @param lineNum
     *     line number from source code (used in error message)
     * @param theLine
     *     String containing source code
     * @param doEqvSubstitutes
     *     boolean param set true to perform .eqv substitutions,
     *     else false
     * @return the generated token list for that line
     */
    private @NotNull TokenList tokenizeLineImpl(
        final @Nullable RISCVProgram program, final int lineNum,
        final @NotNull String theLine, final boolean doEqvSubstitutes
    ) {
        TokenList result = new TokenList();
        if (theLine.isEmpty()) {
            return result;
        }
        // will be faster to work with char arrays instead of strings
        char c;
        final char[] line = theLine.toCharArray();
        int linePos = 0;
        final char[] token = new char[line.length];
        int tokenPos = 0;
        int tokenStartPos = 1;
        boolean insideQuotedString = false;
        if (Globals.debug) {
            Tokenizer.LOGGER.debug("source line --->{}<---", theLine);
        }
        // Each iteration of this loop processes one character in the source line.
        while (linePos < line.length) {
            c = line[linePos];
            if (insideQuotedString) { // everything goes into token
                token[tokenPos++] = c;
                if (c == '"' && token[tokenPos - 2] != '\\') { // If quote not preceded by backslash, this is end
                    this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                    tokenPos = 0;
                    insideQuotedString = false;
                }
            } else { // not inside a quoted string, so be sensitive to delimiters
                switch (c) {
                    case '#': // # denotes comment that takes remainder of line
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                        }
                        tokenStartPos = linePos + 1;
                        tokenPos = line.length - linePos;
                        System.arraycopy(line, linePos, token, 0, tokenPos);
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        linePos = line.length;
                        tokenPos = 0;
                        break;
                    case ' ':
                    case '\t':
                    case ',': // space, tab or comma is delimiter
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }
                        break;
                    // These two guys are special. Will be recognized as unary if and only if two
                    // conditions hold:
                    // 1. Immediately followed by a digit (will use look-ahead for this).
                    // 2. Previous token, if any, is _not_ an IDENTIFIER
                    // Otherwise considered binary and thus a separate token. This is a slight hack
                    // but reasonable.
                    case '+':
                    case '-':
                        // Here's the REAL hack: recognizing signed exponent in E-notation floating
                        // point!
                        // (e.g. 1.2e-5) Add the + or - to the token and keep going. DPS 17 Aug 2005
                        if (tokenPos > 0 && line.length >= linePos + 2 && Character.isDigit(line[linePos + 1]) &&
                            (line[linePos - 1] == 'e' || line[linePos - 1] == 'E')) {
                            token[tokenPos++] = c;
                            break;
                        }
                        // End of REAL hack.
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }

                        tokenStartPos = linePos + 1;
                        token[tokenPos++] = c;
                        if (line.length > linePos + 3 && line[linePos + 1] == 'I' && line[linePos + 2] == 'n'
                            && line[linePos + 3] == 'f') {
                            result.add(new Token(TokenType.REAL_NUMBER, "-Inf", program, lineNum, tokenStartPos));
                            linePos += 3;
                            tokenPos = 0;
                            break;
                        }
                        if (!(
                            (result.isEmpty() || result.get(result.size() - 1).getType() != TokenType.IDENTIFIER) &&
                                (line.length >= linePos + 2 && Character.isDigit(line[linePos + 1]))
                        )) {
                            // treat it as binary.....
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }
                        break;
                    // these are other single-character tokens
                    case ':':
                    case '(':
                    case ')':
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }
                        tokenStartPos = linePos + 1;
                        token[tokenPos++] = c;
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                        break;
                    case '"': // we're not inside a quoted string, so start a new token...
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }
                        tokenStartPos = linePos + 1;
                        token[tokenPos++] = c;
                        insideQuotedString = true;
                        break;
                    case '\'': // start of character constant (single quote).
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                        }
                        // Our strategy is to process the whole thing right now...
                        tokenStartPos = linePos + 1;
                        token[tokenPos++] = c; // Put the quote in token[0]
                        final int lookaheadChars = line.length - linePos - 1;
                        // need minimum 2 more characters, 1 for char and 1 for ending quote
                        if (lookaheadChars < 2) {
                            break; // gonna be an error
                        }
                        c = line[++linePos];
                        token[tokenPos++] = c; // grab second character, put it in token[1]
                        if (c == '\'') {
                            break; // gonna be an error: nothing between the quotes
                        }
                        c = line[++linePos];
                        token[tokenPos++] = c; // grab third character, put it in token[2]
                        // Process if we've either reached second, non-escaped, quote or end of line.
                        if (c == '\'' && token[1] != '\\' || lookaheadChars == 2) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                            tokenStartPos = linePos + 1;
                            break;
                        }
                        // At this point, there is at least one more character on this line. If we're
                        // still here after seeing a second quote, it was escaped. Not done yet;
                        // we either have an escape code, an octal code (also escaped) or invalid.
                        c = line[++linePos];
                        token[tokenPos++] = c; // grab fourth character, put it in token[3]
                        // Process, if this is ending quote for escaped character or if at end of line
                        if (c == '\'' || lookaheadChars == 3) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            );
                            tokenPos = 0;
                            tokenStartPos = linePos + 1;
                            break;
                        }
                        // At this point, we've handled all legal possibilities except octal, e.g.
                        // '\377'
                        // Proceed, if enough characters remain to finish off octal.
                        if (lookaheadChars >= 5) {
                            c = line[++linePos];
                            token[tokenPos++] = c; // grab fifth character, put it in token[4]
                            if (c != '\'') {
                                // still haven't reached end, last chance for validity!
                                c = line[++linePos];
                                token[tokenPos++] = c; // grab sixth character, put it in token[5]
                            }
                        }
                        // process no matter what...we either have a valid character by now or not
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                        tokenStartPos = linePos + 1;
                        break;
                    default:
                        if (tokenPos == 0) {
                            tokenStartPos = linePos + 1;
                        }
                        token[tokenPos++] = c;
                        break;
                } // switch
            } // if (insideQuotedString)
            linePos++;
        } // while
        if (tokenPos > 0) {
            if (insideQuotedString) {
                this.errors.add(ErrorMessage.error(
                    program, lineNum, tokenStartPos,
                    "String is not terminated."
                ));
            }
            this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
        }
        if (doEqvSubstitutes) {
            result = this.processEqv(program, lineNum, theLine, result); // DPS 11-July-2012
        }
        return result;
    }

    /**
     * pre-pre-processing pass through source code to process any ".include"
     * directives.
     * When one is encountered, the contents of the included file are inserted at
     * that
     * point. If no .include statements, the return value is a new array list but
     * with the same lines of source code. Uses recursion to correctly process
     * included
     * files that themselves have .include. Plus it will detect and report recursive
     * includes both direct and indirect.
     * DPS 11-Jan-2013
     */
    private @NotNull List<SourceLine> processIncludes(
        final @NotNull RISCVProgram program,
        final @NotNull Map<String, String> inclFiles
    )
        throws AssemblyException {
        final var sourceList = program.getSourceList();
        final ArrayList<SourceLine> result = new ArrayList<>(sourceList.size());
        for (int i = 0; i < sourceList.size(); i++) {
            final String line = sourceList.get(i);
            final TokenList tokenizedLine = this.tokenizeLineImpl(program, i + 1, line, false);
            boolean hasInclude = false;
            for (int ii = 0; ii < tokenizedLine.size(); ii++) {
                if (tokenizedLine.get(ii).getText().equalsIgnoreCase(Directive.INCLUDE.getName())
                    && (tokenizedLine.size() > ii + 1)
                    && tokenizedLine.get(ii + 1).getType() == TokenType.QUOTED_STRING) {
                    String filename = tokenizedLine
                        .get(ii + 1)
                        .getText()
                        .transform((s) -> s.substring(1, s.length() - 1));
                    // Handle either absolute or relative pathname for .include file
                    if (!new File(filename).isAbsolute()) {
                        filename = program.getFile().getParent() + File.separator + filename;
                    }
                    if (inclFiles.containsKey(filename)) {
                        // This is a recursive include. Generate error message and return immediately.
                        final Token t = tokenizedLine.get(ii + 1);
                        this.errors.add(ErrorMessage.error(
                            program, t.getSourceLine(), t.getStartPos(),
                            "Recursive include of file " + filename
                        ));
                        throw new AssemblyException(this.errors);
                    }
                    inclFiles.put(filename, filename);
                    final RISCVProgram incl = new RISCVProgram();
                    try {
                        incl.readSource(new File(filename));
                    } catch (final AssemblyException p) {
                        final Token token = tokenizedLine.get(ii + 1);
                        this.errors.addTokenError(token, "Error reading include file " + filename);
                        throw new AssemblyException(this.errors);
                    }
                    final var allLines = this.processIncludes(incl, inclFiles);
                    result.addAll(allLines);
                    hasInclude = true;
                    break;
                }
            }
            if (!hasInclude) {
                result.add(new SourceLine(line, program, i + 1));// line);
            }
        }
        return result;
    }

    private @NotNull TokenList processEqv(
        final @Nullable RISCVProgram program, final int lineNum,
        @NotNull String theLine,
        final @NotNull TokenList tokens
    ) {
        // See if it is .eqv directive. If so, record it...
        // Have to assure it is a well-formed statement right now (can't wait for
        // assembler).

        if (tokens.size() > 2 && (
            tokens.get(0).getType() == TokenType.DIRECTIVE
                || tokens.get(2).getType() == TokenType.DIRECTIVE
        )) {
            // There should not be a label but if there is, the directive is in token
            // position 2 (ident, colon, directive).
            final int dirPos = (tokens.get(0).getType() == TokenType.DIRECTIVE) ? 0 : 2;
            if (Directive.matchDirective(tokens.get(dirPos).getText()) == Directive.EQV) {
                // Get position in token list of last non-comment token
                final int tokenPosLastOperand = tokens.size()
                    - ((tokens.get(tokens.size() - 1).getType() == TokenType.COMMENT) ? 2 : 1);
                // There have to be at least two non-comment tokens beyond the directive
                if (tokenPosLastOperand < dirPos + 2) {
                    this.errors.add(ErrorMessage.error(
                        program, lineNum, tokens.get(dirPos).getStartPos(),
                        "Too few operands for " + Directive.EQV.getName() + " directive"
                    ));
                    return tokens;
                }
                // Token following the directive has to be IDENTIFIER
                if (tokens.get(dirPos + 1).getType() != TokenType.IDENTIFIER) {
                    this.errors.add(ErrorMessage.error(
                        program, lineNum, tokens.get(dirPos).getStartPos(),
                        "Malformed " + Directive.EQV.getName() + " directive"
                    ));
                    return tokens;
                }
                final String symbol = tokens.get(dirPos + 1).getText();
                // Make sure the symbol is not contained in the expression. Not likely to occur
                // but if left
                // undetected it will result in infinite recursion. e.g. .eqv ONE, (ONE)
                for (int i = dirPos + 2; i < tokens.size(); i++) {
                    if (tokens.get(i).getText().equals(symbol)) {
                        this.errors.add(ErrorMessage.error(
                            program, lineNum, tokens.get(dirPos).getStartPos(),
                            "Cannot substitute " + symbol + " for itself in " + Directive.EQV.getName()
                                + " directive"
                        ));
                        return tokens;
                    }
                }
                // Expected syntax is symbol, expression. I'm allowing the expression to
                // comprise
                // multiple tokens, so I want to get everything from the IDENTIFIER to either
                // the
                // COMMENT or to the end.
                final int startExpression = tokens.get(dirPos + 2).getStartPos();
                final int endExpression = tokens.get(tokenPosLastOperand).getStartPos()
                    + tokens.get(tokenPosLastOperand).getText().length();
                final String expression = theLine.substring(startExpression - 1, endExpression - 1);
                // Removed equivalents checking - this is a tokenizer, not a semantic checker
                this.equivalents.put(symbol, expression);
                return tokens;
            }
        }
        // Check if a substitution from defined .eqv is to be made. If so, make one.
        boolean substitutionMade = false;
        for (int i = 0; i < tokens.size(); i++) {
            final Token token = tokens.get(i);
            if (token.getType() == TokenType.IDENTIFIER
                && this.equivalents.containsKey(token.getText())) {
                // do the substitution
                final String sub = this.equivalents.get(token.getText());
                final int startPos = token.getStartPos();
                theLine = theLine.substring(0, startPos - 1) + sub
                    + theLine.substring(startPos + token.getText().length() - 1);
                substitutionMade = true; // one substitution per call. If there are multiple, will catch next one on the
                // recursion
                break;
            }
        }
        tokens.setProcessedLine(theLine); // DPS 03-Jan-2013. Related to changes of 11-July-2012.

        return substitutionMade ? this.tokenizeLineImpl(this.program, lineNum, theLine, true) : tokens;
    }

    /// Given candidate token and its position, will classify and record it.
    private void processCandidateToken(
        final char[] token, final @Nullable RISCVProgram program, final int line,
        final @NotNull String theLine,
        final int tokenPos, final int tokenStartPos,
        final @NotNull TokenList tokenList
    ) {
        String value = new String(token, 0, tokenPos);
        if (!value.isEmpty() && value.charAt(0) == '\'') {
            value = preprocessCharacterLiteral(value);
        }
        final TokenType type = TokenType.matchTokenType(value);
        if (type == TokenType.ERROR) {
            this.errors.add(ErrorMessage.error(
                program, line, tokenStartPos,
                theLine + "\nInvalid language element: " + value
            ));
        }
        final Token toke = new Token(type, value, program, line, tokenStartPos);
        tokenList.add(toke);
    }
}
