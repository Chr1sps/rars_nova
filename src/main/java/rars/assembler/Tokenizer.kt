package rars.assembler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.ErrorList
import rars.ErrorMessage
import rars.Globals
import rars.RISCVProgram
import rars.exceptions.AssemblyError
import java.io.File

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
 * Tokens are defined in the TokenTypes class.<br></br>
 * <br></br>
 * Example: <br></br>
 * The RISCV statement
 * `here:  lw  t3, 8(t4)   #load third member of array`<br></br>
 * generates the following token list<br></br>
 * IDENTIFIER, COLON, OPERATOR, REGISTER_NAME, COMMA, INTEGER_5, LEFT_PAREN,
 * REGISTER_NAME, RIGHT_PAREN, COMMENT<br></br>
 *
 * @author Pete Sanderson
 * @version August 2003
 */
class Tokenizer private constructor(
    private val program: RISCVProgram? = null,
    private val errors: ErrorList = ErrorList()
) {
    private val equivalents = mutableMapOf<String, String>()

    // Modified for release 4.3, to preserve existing API.
    /**
     * Will tokenize one line of source code. If lexical errors are discovered,
     * they are noted in an ErrorMessage object which is added to the provided
     * ErrorList
     * instead of the Tokenizer's error list. Will NOT throw an exception.
     *
     * @param program
     * RISCVprogram containing this line of source
     * @param lineNum
     * line number from source code (used in error message)
     * @param theLine
     * String containing source code
     * @param doEqvSubstitutes
     * boolean param set true to perform .eqv substitutions,
     * else false
     * @return the generated token list for that line
     */
    private fun tokenizeLineImpl(
        program: RISCVProgram?,
        lineNum: Int,
        theLine: String,
        doEqvSubstitutes: Boolean
    ): TokenList {
        var result = TokenList()
        if (theLine.isEmpty()) {
            return result
        }
        // will be faster to work with char arrays instead of strings
        val line = theLine.toCharArray()
        val token = CharArray(line.size)
        if (Globals.debug) {
            LOGGER.debug("source line --->{}<---", theLine)
        }
        // Each iteration of this loop processes one character in the source line.
        var insideQuotedString = false
        var tokenStartPos = 1
        var tokenPos = 0
        var linePos = 0
        while (linePos < line.size) {
            var c = line[linePos]
            if (insideQuotedString) {
                // everything goes into token
                token[tokenPos++] = c
                if (c == '"' && token[tokenPos - 2] != '\\') {
                    // If quote not preceded by backslash, this is end
                    this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result)
                    tokenPos = 0
                    insideQuotedString = false
                }
            } else {
                // not inside a quoted string, so be sensitive to delimiters
                when (c) {
                    '#' -> {
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            )
                        }
                        tokenStartPos = linePos + 1
                        tokenPos = line.size - linePos
                        System.arraycopy(line, linePos, token, 0, tokenPos)
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result)
                        linePos = line.size
                        tokenPos = 0
                    }

                    ' ', '\t', ',' -> if (tokenPos > 0) {
                        this.processCandidateToken(
                            token, program, lineNum, theLine, tokenPos, tokenStartPos,
                            result
                        )
                        tokenPos = 0
                    }

                    '+', '-' -> {
                        // Here's the REAL hack: recognizing signed exponent in E-notation floating
                        // point!
                        // (e.g. 1.2e-5) Add the + or - to the token and keep going.
                        if (tokenPos > 0 &&
                            line.size >= linePos + 2 &&
                            Character.isDigit(line[linePos + 1]) &&
                            (line[linePos - 1] == 'e' || line[linePos - 1] == 'E')
                        ) {
                            token[tokenPos++] = c
                        } else {
                            if (tokenPos > 0) {
                                this.processCandidateToken(
                                    token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                    result
                                )
                                tokenPos = 0
                            }

                            tokenStartPos = linePos + 1
                            token[tokenPos++] = c
                            if (line.size > linePos + 3 &&
                                line[linePos + 1] == 'I' &&
                                line[linePos + 2] == 'n' &&
                                line[linePos + 3] == 'f'
                            ) {
                                result.add(Token(TokenType.REAL_NUMBER, "-Inf", program, lineNum, tokenStartPos))
                                linePos += 3
                                tokenPos = 0
                            } else if (
                                !((result.isEmpty() || result.last().type != TokenType.IDENTIFIER) &&
                                    (line.size >= linePos + 2 && line[linePos + 1].isDigit()))
                            ) {
                                // treat it as binary.....
                                this.processCandidateToken(
                                    token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                    result
                                )
                                tokenPos = 0
                            }

                        }
                    }

                    ':', '(', ')' -> {
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            )
                            tokenPos = 0
                        }
                        tokenStartPos = linePos + 1
                        token[tokenPos++] = c
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result)
                        tokenPos = 0
                    }

                    '"' -> {
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            )
                            tokenPos = 0
                        }
                        tokenStartPos = linePos + 1
                        token[tokenPos++] = c
                        insideQuotedString = true
                    }

                    '\'' -> {
                        if (tokenPos > 0) {
                            this.processCandidateToken(
                                token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                result
                            )
                            tokenPos = 0
                        }
                        // Our strategy is to process the whole thing right now...
                        tokenStartPos = linePos + 1
                        token[tokenPos++] = c // Put the quote in token[0]
                        val lookaheadChars = line.size - linePos - 1
                        // need minimum 2 more characters, 1 for char and 1 for ending quote
                        if (lookaheadChars >= 2) {
                            c = line[++linePos]
                            token[tokenPos++] = c // grab second character, put it in token[1]
                            if (c != '\'') {
                                c = line[++linePos]
                                token[tokenPos++] = c // grab third character, put it in token[2]
                                // Process if we've either reached second, non-escaped, quote or end of line.
                                if (c == '\'' && token[1] != '\\' || lookaheadChars == 2) {
                                    this.processCandidateToken(
                                        token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                        result
                                    )
                                    tokenPos = 0
                                    tokenStartPos = linePos + 1
                                } else {
                                    // At this point, there is at least one more character on this line. If we're
                                    // still here after seeing a second quote, it was escaped. Not done yet;
                                    // we either have an escape code, an octal code (also escaped) or invalid.
                                    c = line[++linePos]
                                    token[tokenPos++] = c // grab fourth character, put it in token[3]
                                    // Process, if this is ending quote for escaped character or if at end of line
                                    if (c == '\'' || lookaheadChars == 3) {
                                        this.processCandidateToken(
                                            token, program, lineNum, theLine, tokenPos, tokenStartPos,
                                            result
                                        )
                                        tokenPos = 0
                                        tokenStartPos = linePos + 1
                                    } else {
                                        // At this point, we've handled all legal possibilities except octal, e.g.
                                        // '\377'
                                        // Proceed, if enough characters remain to finish off octal.
                                        if (lookaheadChars >= 5) {
                                            c = line[++linePos]
                                            token[tokenPos++] = c // grab fifth character, put it in token[4]
                                            if (c != '\'') {
                                                // still haven't reached end, last chance for validity!
                                                c = line[++linePos]
                                                token[tokenPos++] = c // grab sixth character, put it in token[5]
                                            }
                                        }
                                        // process no matter what...we either have a valid character by now or not
                                        this.processCandidateToken(
                                            token,
                                            program,
                                            lineNum,
                                            theLine,
                                            tokenPos,
                                            tokenStartPos,
                                            result
                                        )
                                        tokenPos = 0
                                        tokenStartPos = linePos + 1
                                    }
                                }
                            }
                            // gonna be an error: nothing between the quotes
                        }
                    }

                    else -> {
                        if (tokenPos == 0) {
                            tokenStartPos = linePos + 1
                        }
                        token[tokenPos++] = c
                    }
                }
            }

            linePos++
        }

        if (tokenPos > 0) {
            if (insideQuotedString) {
                this.errors.add(
                    ErrorMessage.error(
                        program, lineNum, tokenStartPos,
                        "String is not terminated."
                    )
                )
            }
            this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result)
        }
        if (doEqvSubstitutes) {
            result = this.processEqv(program, lineNum, theLine, result)
        }
        return result
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
     */
    private fun processIncludes(
        program: RISCVProgram,
        inclFiles: MutableMap<String, String>
    ): Either<AssemblyError, MutableList<SourceLine>> = either {
        val sourceList = program.sourceList!!
        val result = mutableListOf<SourceLine>()
        for (i in sourceList.indices) {
            val line = sourceList[i]
            val tokenizedLine = tokenizeLineImpl(program, i + 1, line, false)
            var hasInclude = false
            for (ii in tokenizedLine.indices) {
                if (tokenizedLine.get(ii).text.equals(Directive.INCLUDE.directiveName, ignoreCase = true)
                    && (tokenizedLine.size > ii + 1)
                    && tokenizedLine.get(ii + 1).type == TokenType.QUOTED_STRING
                ) {
                    var filename: String = tokenizedLine
                        .get(ii + 1)
                        .text.let { it.substring(1, it.length - 1) }
                    // Handle either absolute or relative pathname for .include file
                    if (!File(filename).isAbsolute) {
                        filename = program.file!!.getParent() + File.separator + filename
                    }
                    if (inclFiles.containsKey(filename)) {
                        // This is a recursive include. Generate error message and return immediately.
                        val t = tokenizedLine.get(ii + 1)
                        errors.add(
                            ErrorMessage.error(
                                program, t.sourceLine, t.startPos,
                                "Recursive include of file $filename"
                            )
                        )
                        this.raise(AssemblyError(errors))
                    }
                    inclFiles.put(filename, filename)
                    val incl = RISCVProgram()
                    incl.readSource(File(filename)).onLeft {
                        val token = tokenizedLine.get(ii + 1)
                        this@Tokenizer.errors.addTokenError(token, "Error reading include file $filename")
                        raise(AssemblyError(this@Tokenizer.errors))
                    }
                    val allLines = this@Tokenizer.processIncludes(incl, inclFiles).bind()
                    result.addAll(allLines)
                    hasInclude = true
                    break
                }
            }
            if (!hasInclude) {
                result.add(SourceLine(line, program, i + 1)) // line);
            }
        }
        result
    }

    private fun processEqv(
        program: RISCVProgram?,
        lineNum: Int,
        theLine: String,
        tokens: TokenList
    ): TokenList {
        // See if it is .eqv directive. If so, record it...
        // Have to assure it is a well-formed statement right now (can't wait for
        // assembler).

        var theLine = theLine
        if (tokens.size > 2 && (tokens.get(0).type == TokenType.DIRECTIVE
                || tokens.get(2).type == TokenType.DIRECTIVE
                )
        ) {
            // There should not be a label but if there is, the directive is in token
            // position 2 (ident, colon, directive).
            val dirPos = if (tokens.get(0).type == TokenType.DIRECTIVE) 0 else 2
            if (Directive.matchDirective(tokens.get(dirPos).text) == Directive.EQV) {
                // Get position in token list of last non-comment token
                val tokenPosLastOperand = (tokens.size
                    - (if (tokens.get(tokens.size - 1).type == TokenType.COMMENT) 2 else 1))
                // There have to be at least two non-comment tokens beyond the directive
                if (tokenPosLastOperand < dirPos + 2) {
                    this.errors.add(
                        ErrorMessage.error(
                            program, lineNum, tokens.get(dirPos).startPos,
                            "Too few operands for " + Directive.EQV.directiveName + " directive"
                        )
                    )
                    return tokens
                }
                // Token following the directive has to be IDENTIFIER
                if (tokens.get(dirPos + 1).type != TokenType.IDENTIFIER) {
                    this.errors.add(
                        ErrorMessage.error(
                            program, lineNum, tokens.get(dirPos).startPos,
                            "Malformed " + Directive.EQV.directiveName + " directive"
                        )
                    )
                    return tokens
                }
                val symbol = tokens.get(dirPos + 1).text
                // Make sure the symbol is not contained in the expression. Not likely to occur
                // but if left
                // undetected it will result in infinite recursion. e.g. .eqv ONE, (ONE)
                for (i in dirPos + 2..<tokens.size) {
                    if (tokens.get(i).text == symbol) {
                        this.errors.add(
                            ErrorMessage.error(
                                program, lineNum, tokens.get(dirPos).startPos,
                                ("Cannot substitute " + symbol + " for itself in " + Directive.EQV.directiveName
                                    + " directive")
                            )
                        )
                        return tokens
                    }
                }
                // Expected syntax is symbol, expression. I'm allowing the expression to
                // comprise
                // multiple tokens, so I want to get everything from the IDENTIFIER to either
                // the
                // COMMENT or to the end.
                val startExpression = tokens.get(dirPos + 2).startPos
                val endExpression = (tokens.get(tokenPosLastOperand).startPos
                    + tokens.get(tokenPosLastOperand).text.length)
                val expression = theLine.substring(startExpression - 1, endExpression - 1)
                // Removed equivalents checking - this is a tokenizer, not a semantic checker
                this.equivalents.put(symbol, expression)
                return tokens
            }
        }
        // Check if a substitution from defined .eqv is to be made. If so, make one.
        var substitutionMade = false
        for (i in tokens.indices) {
            val token = tokens.get(i)
            if (token.type == TokenType.IDENTIFIER
                && this.equivalents.containsKey(token.text)
            ) {
                // do the substitution
                val sub = this.equivalents[token.text]
                val startPos = token.startPos
                theLine = (theLine.substring(0, startPos - 1) + sub
                    + theLine.substring(startPos + token.text.length - 1))
                substitutionMade = true // one substitution per call. If there are multiple, will catch next one on the
                // recursion
                break
            }
        }
        tokens.processedLine = theLine

        return if (substitutionMade) this.tokenizeLineImpl(this.program, lineNum, theLine, true) else tokens
    }

    /** Given candidate token and its position, will classify and record it. */
    private fun processCandidateToken(
        token: CharArray, program: RISCVProgram?, line: Int,
        theLine: String,
        tokenPos: Int, tokenStartPos: Int,
        tokenList: TokenList
    ) {
        var value = String(token, 0, tokenPos)
        if (!value.isEmpty() && value[0] == '\'') {
            value = preprocessCharacterLiteral(value)
        }
        val type = TokenType.matchTokenType(value)
        if (type == TokenType.ERROR) {
            this.errors.add(
                ErrorMessage.error(
                    program, line, tokenStartPos,
                    "$theLine\nInvalid language element: $value"
                )
            )
        }
        val toke = Token(type, value, program, line, tokenStartPos)
        tokenList.add(toke)
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(Tokenizer::class.java)

        // The 8 escaped characters are: single quote, double quote, backslash, newline
        // (linefeed),
        // tab, backspace, return, form feed. The characters and their corresponding
        // decimal codes:
        private const val ESCAPED_CHARACTERS = "'\"\\ntbrf0"
        private val escapedCharactersValues = arrayOf<String>("39", "34", "92", "10", "9", "8", "13", "12", "0")

        /** If passed a candidate character literal, attempt to translate it into integer
         * constant.
         * If the translation fails, return original value. */
        private fun preprocessCharacterLiteral(value: String): String {
            // must start and end with quote and have something in between
            if (value.length < 3 || value[0] != '\'' || value[value.length - 1] != '\'') {
                return value
            }
            val quotesRemoved = value.substring(1, value.length - 1)
            // if not escaped, then if one character left return its value else return
            // original.
            if (quotesRemoved[0] != '\\') {
                return if (quotesRemoved.length == 1) quotesRemoved[0].code.toString() else value
            }
            // now we know it is escape sequence and have to decode which of the 8:
            // ',",\,n,t,b,r,f
            if (quotesRemoved.length == 2) {
                val escapedCharacterIndex: Int = ESCAPED_CHARACTERS.indexOf(quotesRemoved[1])
                return if (escapedCharacterIndex >= 0) escapedCharactersValues[escapedCharacterIndex] else value
            }
            // last valid possibility is 3 digit octal code 000 through 377
            if (quotesRemoved.length == 4) {
                try {
                    val intValue = quotesRemoved.substring(1).toInt(8)
                    if (intValue >= 0 && intValue <= 255) {
                        return intValue.toString()
                    }
                } catch (_: NumberFormatException) {
                } // if not valid octal, will fall through and reject
            }
            return value
        }

        /** Will tokenize a complete source program.
         *
         * @param program
         * The [RISCVProgram] to be tokenized.
         * @return A [List] representing the tokenized program. Each list member is a [TokenList].
         * that represents a tokenized source statement from the program.
         * @throws AssemblyError
         * if any.
         */
        @JvmStatic
        fun tokenize(program: RISCVProgram): Either<AssemblyError, List<TokenList>> = either {
            val tokenizer = Tokenizer(program)
            val source = tokenizer.processIncludes(program, mutableMapOf()).bind()
            program.sourceLineList = source

            val tokenList = mutableListOf<TokenList>()
            for (i in source.indices) {
                val sourceLine = source[i].source
                val currentLineTokens = tokenizer.tokenizeLineImpl(tokenizer.program, i + 1, sourceLine, true)
                tokenList.add(currentLineTokens)
                // If source code substitution was
                // made
                // based on .eqv directive during tokenizing, the processed line, a String, is
                // not the same object as the original line. Thus I can use != instead of
                // !equals()
                // This IF statement will replace original source with source modified by .eqv
                // substitution.
                // Not needed by assembler, but looks better in the Text Segment Display.
                if (sourceLine.isNotEmpty() && sourceLine != currentLineTokens.processedLine) {
                    source[i] = SourceLine(
                        currentLineTokens.processedLine,
                        source[i].program,
                        source[i].lineNumber
                    )
                }
            }
            ensure(!tokenizer.errors.errorsOccurred()) { AssemblyError(tokenizer.errors) }
            tokenList
        }

        /** Used only to create a token list for the example provided with each
         * instruction specification.
         *
         * @param example
         * The example RISCV instruction to be tokenized.
         * @return A [TokenList] object representing the tokenized instruction.
         * @throws AssemblyError
         * This occurs only if the instruction
         * specification itself contains one or more
         * lexical (i.e. token) errors.
         */
        @JvmStatic
        fun tokenizeExampleInstruction(example: String): Either<AssemblyError, TokenList> = either {
            val tokenizer = Tokenizer()
            val result = tokenizer.tokenizeLineImpl(null, 0, example, false)
            ensure(!tokenizer.errors.errorsOccurred()) { AssemblyError(tokenizer.errors) }
            result
        }

        /**
         * Will tokenize one line of source code. If lexical errors are discovered,
         * they are noted in an ErrorMessage object which is added to the provided
         * ErrorList
         * instead of the Tokenizer's error list. Will NOT throw an exception.
         *
         * @param lineNum
         * line number from source code (used in error message)
         * @param theLine
         * String containing source code
         * @param callerErrorList
         * errors will go into this list instead of tokenizer's
         * list.
         * @param doEqvSubstitutes
         * boolean param set true to perform .eqv substitutions,
         * else false
         * @return the generated token list for that line
         */
        @JvmStatic
        fun tokenizeLine(
            lineNum: Int,
            theLine: String,
            callerErrorList: ErrorList,
            doEqvSubstitutes: Boolean
        ): TokenList {
            val tokenizer = Tokenizer(null, callerErrorList)
            return tokenizer.tokenizeLineImpl(null, lineNum, theLine, doEqvSubstitutes)
        }
    }
}
