package rars.assembler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.*
import rars.events.AssemblyError
import rars.riscv.BasicInstruction
import rars.riscv.ExtendedInstruction
import rars.riscv.Instruction
import rars.riscv.InstructionsRegistry
import rars.settings.BoolSetting
import rars.util.toHexStringWithPrefix
import rars.util.translateToInt
import rars.util.translateToLong
import rars.venus.NumberDisplayBaseChooser

/**
 * An Assembler is capable of assembling a RISCV program. It has only one public
 * method, `assemble()`, which implements a two-pass assembler. It
 * translates RISCV source code into binary machine code.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
class Assembler {
    private var errors: ErrorList? = null
    private var inDataSegment = false // status maintained by parser
    private var inMacroSegment = false // status maintained by parser, true if in

    // macro definition segment
    private var externAddress = 0
    private var autoAlign = false
    private var dataDirective: Directive? = null
    private var fileCurrentlyBeingAssembled: RISCVProgram? = null
    private var globalDeclarationList: TokenList? = null
    private var textAddress = 0
    private var dataAddress = 0
    private var currentFileDataSegmentForwardReferenceList: DataSegmentForwardReferenceList? = null

    val errorList: ErrorList
        /**
         * Get list of assembler errors and warnings
         *
         * @return ErrorList of any assembler errors and warnings.
         */
        get() = this.errors!!

    /**
     * Parse and generate machine code for the given program. All source
     * files must have already been tokenized.
     *
     * @param tokenizedProgramFiles
     * An ArrayList of RISCVprogram objects, each
     * produced from a
     * different source code file, representing the
     * program source.
     * @param extendedAssemblerEnabled
     * A boolean value that if true permits use of
     * extended (pseudo)
     * instructions in the source code. If false,
     * these are flagged
     * as errors.
     * @param warningsAreErrors
     * A boolean value - true means assembler
     * warnings will be
     * considered errors and terminate the assemble;
     * false means the
     * assembler will produce warning message but
     * otherwise ignore
     * warnings.
     * @return An ArrayList representing the assembled program. Each member of
     * the list is a ProgramStatement object containing the source,
     * intermediate, and machine binary representations of a program
     * statement. Returns null if incoming array list is null or empty.
     * @throws AssemblyError
     * if any.
     * @see ProgramStatement
     */
    private fun assembleImpl(
        tokenizedProgramFiles: List<RISCVProgram>,
        extendedAssemblerEnabled: Boolean,
        warningsAreErrors: Boolean
    ): Either<AssemblyError, List<ProgramStatement>> = either {
        check(!tokenizedProgramFiles.isEmpty()) { "No source code to assemble." }
        val memoryConfiguration = Globals.MEMORY_INSTANCE.memoryConfiguration
        this@Assembler.textAddress = memoryConfiguration.textBaseAddress
        this@Assembler.dataAddress = memoryConfiguration.dataBaseAddress
        this@Assembler.externAddress = memoryConfiguration.externBaseAddress
        this@Assembler.currentFileDataSegmentForwardReferenceList = DataSegmentForwardReferenceList()
        val accumulatedDataSegmentForwardReferenceList = DataSegmentForwardReferenceList()
        Globals.GLOBAL_SYMBOL_TABLE.clear()
        Globals.MEMORY_INSTANCE.reset()
        this@Assembler.errors = ErrorList()
        if (Globals.debug) {
            LOGGER.debug("Assembler first pass begins:")
        }

        for (program in tokenizedProgramFiles) {
            this@Assembler.checkEqvDirectives(program)
        }

        // PROCESS THE FIRST ASSEMBLY PASS FOR ALL SOURCE FILES BEFORE PROCEEDING
        // TO SECOND PASS. THIS ASSURES ALL SYMBOL TABLES ARE CORRECTLY BUILT.
        // THERE IS ONE GLOBAL SYMBOL TABLE (for identifiers declared .globl) PLUS
        // ONE LOCAL SYMBOL TABLE FOR EACH SOURCE FILE.
        for (tokenizedProgram in tokenizedProgramFiles) {
            if (this@Assembler.errors!!.errorLimitExceeded()) {
                break
            }
            this@Assembler.fileCurrentlyBeingAssembled = tokenizedProgram
            // List of labels declared ".globl". new list for each file assembled
            this@Assembler.globalDeclarationList = TokenList()
            // Parser begins by default in text segment until directed otherwise.
            this@Assembler.inDataSegment = false
            // Macro segment will be started by .macro directive
            this@Assembler.inMacroSegment = false
            // Default is to align data from directives on appropriate boundary (word, half,
            // byte)
            // This can be turned off for remainder of current data segment with ".align 0"
            this@Assembler.autoAlign = true
            // Default data directive is .word for 4 byte data items
            this@Assembler.dataDirective = Directive.WORD
            // Clear out (initialize) symbol table related structures.
            this@Assembler.fileCurrentlyBeingAssembled!!.localSymbolTable!!.clear()
            this@Assembler.currentFileDataSegmentForwardReferenceList!!.clear()
            // sourceList is an ArrayList of String objects, one per source line.
            // tokenList is an ArrayList of TokenList objects, one per source line;
            // each ArrayList in tokenList consists of Token objects.
            val sourceLineList = this@Assembler.fileCurrentlyBeingAssembled!!.sourceLineList!!
            val tokenList = this@Assembler.fileCurrentlyBeingAssembled!!.tokenList!!
            val parsedList = mutableListOf<ProgramStatement>()
            // each file keeps its own macro definitions
            this@Assembler.fileCurrentlyBeingAssembled!!.createMacroPool()
            // FIRST PASS OF ASSEMBLER VERIFIES SYNTAX, GENERATES SYMBOL TABLE,
            // INITIALIZES DATA SEGMENT
            for (i in tokenList.indices) {
                if (this@Assembler.errors!!.errorLimitExceeded()) {
                    break
                }
                for (t in tokenList[i]) {
                    // record this token's original source program and line #. Differs from final,
                    // if .include used
                    t.setOriginal(sourceLineList[i].lineNumber)
                }
                val statements = this@Assembler.parseLine(
                    tokenList[i],
                    sourceLineList[i].source,
                    sourceLineList[i].lineNumber,
                    extendedAssemblerEnabled
                )
                if (statements != null) {
                    parsedList.addAll(statements)
                }
            }
            this@Assembler.fileCurrentlyBeingAssembled!!.parsedList = parsedList
            if (this@Assembler.inMacroSegment) {
                this@Assembler.errors!!.add(
                    ErrorMessage.error(
                        this@Assembler.fileCurrentlyBeingAssembled,
                        this@Assembler.fileCurrentlyBeingAssembled!!.localMacroPool!!.current!!.fromLine,
                        0, "Macro started but not ended (no .end_macro directive)"
                    )
                )
            }
            // move ".globl" symbols from local symtab to global
            this@Assembler.transferGlobals()
            // Attempt to resolve forward label references that were discovered in operand
            // fields
            // of data segment directives in current file. Those that are not resolved after
            // this
            // call are either references to global labels not seen yet, or are undefined.
            // Cannot determine which until all files are parsed, so copy unresolved entries
            // into accumulated list and clear out this one for re-use with the next source
            // file.
            this@Assembler.currentFileDataSegmentForwardReferenceList!!.resolve(
                this@Assembler.fileCurrentlyBeingAssembled!!
                    .localSymbolTable!!
            )
            accumulatedDataSegmentForwardReferenceList.add(this@Assembler.currentFileDataSegmentForwardReferenceList!!)
            this@Assembler.currentFileDataSegmentForwardReferenceList!!.clear()
        }


        // Have processed all source files. Attempt to resolve any remaining forward
        // label
        // references from global symbol table. Those that remain unresolved are
        // undefined
        // and require error message.
        accumulatedDataSegmentForwardReferenceList.resolve(Globals.GLOBAL_SYMBOL_TABLE)
        accumulatedDataSegmentForwardReferenceList.generateErrorMessages(this@Assembler.errors!!)

        // Throw collection of errors accumulated through the first pass.
        ensure(!this@Assembler.errors!!.errorsOccurred()) { AssemblyError(this@Assembler.errors!!) }
        if (Globals.debug) {
            LOGGER.debug("Assembler second pass begins")
        }
        // SECOND PASS OF ASSEMBLER GENERATES BASIC ASSEMBLER THEN MACHINE CODE.
        // Generates basic assembler statements...
        val machineList = ArrayList<ProgramStatement>()
        for (program in tokenizedProgramFiles) {
            if (this@Assembler.errors!!.errorLimitExceeded()) {
                break
            }
            this@Assembler.fileCurrentlyBeingAssembled = program
            val parsedList = this@Assembler.fileCurrentlyBeingAssembled!!.parsedList!!
            for (statement in parsedList) {
                statement.buildBasicStatementFromBasicInstruction(this@Assembler.errors)
                ensure(!this@Assembler.errors!!.errorsOccurred()) { AssemblyError(this@Assembler.errors!!) }
                when (val instruction = statement.instruction) {
                    is ExtendedInstruction -> {
                        // It is a 
                        // pseudo-instruction:
                        // 1. Fetch its basic instruction template list
                        // 2. For each template in the list,
                        // 2a. substitute operands from source statement
                        // 2b. tokenize the statement generated by 2a.
                        // 2d. call parseLine() to generate basic instrction
                        // 2e. add returned programStatement to the list
                        // The templates, and the instructions generated by filling
                        // in the templates, are specified
                        // in basic format (e.g. mnemonic register reference zero
                        // already translated to x0).
                        // So the values substituted into the templates need to be
                        // in this format. Since those
                        // values come from the original source statement, they need
                        // to be translated before
                        // substituting. The next method call will perform this
                        // translation on the original
                        // source statement. Despite the fact that the original
                        // statement is a pseudo
                        // instruction, this method performs the necessary
                        // translation correctly.
                        // TODO: consider making this recursive
                        val basicAssembly = statement.basicAssemblyStatement
                        val lineNumber = statement.sourceLine!!.lineNumber
                        val tokenList = Tokenizer.tokenizeLine(
                            lineNumber,
                            basicAssembly, this@Assembler.errors!!, false
                        )

                        // If we are using compact memory config and there is a compact expansion, use
                        // it
                        val templateList = instruction.basicIntructionTemplateList

                        // subsequent ProgramStatement constructor needs the correct text segment address.
                        this@Assembler.textAddress = statement.address
                        // Will generate one basic instruction for each template in the list.
                        val pc = this@Assembler.textAddress // Save the starting PC so that it can be used for PC 
                        // relative stuff
                        for (s in templateList) {
                            val instruction = ExtendedInstruction.makeTemplateSubstitutions(
                                this@Assembler.fileCurrentlyBeingAssembled!!,
                                s, tokenList, pc
                            )

                            // All substitutions have been made so we have generated
                            // a valid basic instruction!
                            if (Globals.debug) {
                                LOGGER.debug("PSEUDO generated: {}", instruction)
                            }
                            // For generated instruction: tokenize, build program
                            // statement, add to list.
                            val newTokenList = Tokenizer.tokenizeLine(
                                lineNumber,
                                instruction, this@Assembler.errors!!, false
                            )
                            val instrMatches = this@Assembler.matchInstruction(newTokenList.get(0))
                            val instr = OperandUtils.bestOperandMatch(
                                newTokenList,
                                instrMatches!!
                            )
                            // Only first generated instruction is linked to original source
                            val ps = ProgramStatement( // this.fileCurrentlyBeingAssembled,
                                // (instrNumber == 0) ? statement.source : "",
                                newTokenList,
                                newTokenList,
                                instr!!,
                                this@Assembler.textAddress,
                                statement.sourceLine
                            )
                            this@Assembler.textAddress += (BasicInstruction.BASIC_INSTRUCTION_LENGTH)
                            ps.buildBasicStatementFromBasicInstruction(this@Assembler.errors)
                            machineList.add(ps)
                        }
                    }

                    else -> {
                        machineList.add(statement)
                    }
                }
            }
        }
        if (Globals.debug) {
            LOGGER.debug("Code generation begins")
        }

        // THIRD MAJOR STEP IS PRODUCE MACHINE CODE FROM ASSEMBLY

        // Generates machine code statements from the list of basic assembler statements
        // and writes the statement to memory.
        for (statement in machineList) {
            if (this@Assembler.errors!!.errorLimitExceeded()) {
                break
            }
            statement.buildMachineStatementFromBasicStatement(this@Assembler.errors!!)
            if (Globals.debug) {
                LOGGER.debug(statement)
            }
            Globals.MEMORY_INSTANCE
                .setProgramStatement(statement.address, statement)
                .onLeft { error ->
                    val token = statement.originalTokenList!![0]
                    errors!!.addTokenError(token, "Invalid address for text segment: ${error.address}")
                }
        }
        // We will now sort the ArrayList of ProgramStatements by getAddress() value.
        // This is for display purposes, since they have already been stored to Memory.
        // Use of .ktext and .text with address operands has two implications:
        // (1) the addresses may not be ordered at this point. Requires unsigned int
        // sort because kernel addresses are negative. See special Comparator.
        // (2) It is possible for two instructions to be placed at the same address.
        // Such occurances will be flagged as errors.
        // Yes, I would not have to sort here if I used SortedSet rather than ArrayList
        // but in case of duplicate I like having both statements handy for error
        // message.
        val sortedMachineList = machineList.sorted()
        catchDuplicateAddresses(sortedMachineList, this@Assembler.errors!!)
        ensure(
            !(this@Assembler.errors!!.errorsOccurred()
                || this@Assembler.errors!!.warningsOccurred()
                && warningsAreErrors)
        ) {
            AssemblyError(this@Assembler.errors!!)
        }
        sortedMachineList
    }

    private fun checkEqvDirectives(program: RISCVProgram) {
        val symbols = mutableListOf<String>()
        val tokens = program.tokenList!!
        for (line in tokens) {
            if (line.size > 2 && (line.get(0).type == TokenType.DIRECTIVE ||
                    line.get(2).type == TokenType.DIRECTIVE
                    )
            ) {
                val dirPos = if (line.get(0).type == TokenType.DIRECTIVE) 0 else 2
                if (Directive.matchDirective(line.get(dirPos).text) == Directive.EQV) {
                    val symbol = line.get(dirPos + 1).text
                    // Symbol cannot be redefined - the only reason for this is to act like the Gnu
                    // .eqv
                    if (symbols.contains(symbol)) {
                        this.errors!!.addTokenError(
                            line.get(dirPos + 1),
                            "Symbol $symbol already defined in this file"
                        )
                    } else {
                        symbols.add(symbol)
                    }
                }
            }
        }
    }

    /**
     * This method parses one line of RISCV source code. It works with the list
     * of tokens, but original source is also provided. It also carries out
     * directives, which includes initializing the data segment. This method is
     * invoked in the assembler first pass.
     *
     * @param tokenList
     * @param source
     * @param sourceLineNumber
     * @param extendedAssemblerEnabled
     * @return ArrayList of ProgramStatements because parsing a macro expansion
     * request will return a list of ProgramStatements expanded
     */
    private fun parseLine(
        tokenList: TokenList,
        source: String,
        sourceLineNumber: Int,
        extendedAssemblerEnabled: Boolean
    ): List<ProgramStatement>? {
        var tokens: TokenList = stripComment(tokenList)

        // Labels should not be processed in macro definition segment.
        val macroPool = this.fileCurrentlyBeingAssembled!!.localMacroPool!!
        if (this.inMacroSegment) {
            detectLabels(tokens, macroPool.current!!)
        } else {
            this.stripLabels(tokens)
        }
        if (tokens.isEmpty()) {
            return null
        }
        // Grab first (operator) token...
        val token = tokens.get(0)
        val tokenType = token.type

        // Let's handle the directives here...
        if (tokenType == TokenType.DIRECTIVE) {
            this.executeDirective(tokens)
            return null
        }

        // don't parse if in macro segment
        if (this.inMacroSegment) {
            return null
        }

        // SPIM-style macro calling:
        var parenFreeTokens = tokens
        if (tokens.size > 2 && tokens.get(1).type == TokenType.LEFT_PAREN && tokens.get(tokens.size - 1)
                .type == TokenType.RIGHT_PAREN
        ) {
            parenFreeTokens = tokens.clone() as TokenList
            parenFreeTokens.remove(tokens.size - 1)
            parenFreeTokens.remove(1)
        }
        val macro = macroPool.getMatchingMacro(parenFreeTokens) // parenFreeTokens.get(0).getSourceLine());

        // expand macro if this line is a macro expansion call
        val result = mutableListOf<ProgramStatement>()
        if (macro != null) {
            tokens = parenFreeTokens
            // get unique id for this expansion
            val counter = macroPool.nextCounter
            if (macroPool.pushOnCallStack(token)) {
                this.errors!!.add(
                    ErrorMessage.error(
                        this.fileCurrentlyBeingAssembled, tokens.get(0)
                            .sourceLine, 0, "Detected a macro expansion loop (recursive reference). "
                    )
                )
            } else {
                for (i in macro.fromLine + 1..<macro.toLine) {
                    var substituted = macro.getSubstitutedLine(i, tokens, counter.toLong(), this.errors)
                    val tokenList2 = Tokenizer.tokenizeLine(
                        i, substituted, this.errors!!, true
                    )

                    // If token list getProcessedLine() is not empty, then .eqv was performed and it
                    // contains the modified source.
                    // Put it into the line to be parsed, so it will be displayed properly in text
                    // segment display. 
                    if (!tokenList2.processedLine.isEmpty()) {
                        substituted = tokenList2.processedLine
                    }

                    // recursively parse lines of expanded macro
                    val statements = this.parseLine(
                        tokenList2,
                        ("<" + (i - macro.fromLine + macro.originalFromLine) + "> "
                            + substituted.trim { it <= ' ' }),
                        sourceLineNumber, extendedAssemblerEnabled
                    )
                    if (statements != null) {
                        result.addAll(statements)
                    }
                }
                macroPool.popFromCallStack()
            }
            return result
        }

        // TODO: check what gcc and clang generated assembly looks like currently
        // Yet Another Hack: detect unrecognized directive. MARS recognizes the same
        // directives
        // as SPIM but other MIPS assemblers recognize additional directives. Compilers
        // such
        // as MIPS-directed GCC generate assembly code containing these directives. We'd
        // like
        // the opportunity to ignore them and continue. Tokenizer would categorize an
        // unrecognized
        // directive as an TokenTypes.IDENTIFIER because it would not be matched as a
        // directive and
        // MIPS labels can start with '.' NOTE: this can also be handled by including
        // the
        // ignored directive in the Directives.java list. There is already a mechanism
        // in place
        // for generating a warning there. But I cannot anticipate the names of all
        // directives
        // so this will catch anything, including a misspelling of a valid directive
        // (which is
        // a nice thing to do).
        if (tokenType == TokenType.IDENTIFIER && token.text[0] == '.') {
            errors!!.addWarning(
                token,
                "RARS does not recognize the ${token.text} directive. Ignored."
            )
            return null
        }

        // The directives with lists (.byte, .double, .float, .half, .word, .ascii,
        // .asciiz)
        // should be able to extend the list over several lines. Since this method
        // assembles
        // only one source line, state information must be stored from one invocation to
        // the next, to sense the context of this continuation line. That state
        // information
        // is contained in this.dataDirective (the current data directive).
        if (this.inDataSegment &&  // Added data segment guard...
            (tokenType == TokenType.PLUS ||  // because invalid instructions were being caught...
                tokenType == TokenType.MINUS ||  // here and reported as a directive in text segment!
                tokenType == TokenType.QUOTED_STRING ||
                tokenType == TokenType.IDENTIFIER ||
                TokenType.isIntegerTokenType(tokenType) ||
                TokenType.isFloatingTokenType(tokenType))
        ) {
            this.executeDirectiveContinuation(tokens)
            return null
        }

        // If we are in the text segment, the variable "token" must now refer to
        // an OPERATOR
        // token. If not, it is either a syntax error or the specified operator
        // is not
        // yet implemented.
        if (!this.inDataSegment) {
            val instrMatches = this.matchInstruction(token)
            if (instrMatches == null) {
                return result
            }
            // OK, we've got an operator match, let's check the operands.
            val instruction = OperandUtils.bestOperandMatch(tokens, instrMatches)
            // Here's the place to flag use of extended (pseudo) instructions
            // when setting disabled.
            if (instruction is ExtendedInstruction && !extendedAssemblerEnabled) {
                this.errors!!.addTokenError(
                    token,
                    "Extended (pseudo) instruction or format not permitted.  See Settings" +
                        "."
                )
            }
            if (OperandUtils.checkIfTokensMatchOperand(tokens, instruction, this.errors)) {
                val sourceLine = SourceLine(source, this.fileCurrentlyBeingAssembled!!, sourceLineNumber)
                val programStatement = ProgramStatement(
                    tokenList,
                    tokens,
                    instruction!!,
                    this.textAddress,
                    sourceLine
                )
                // instruction length is 4 for all basic instruction, varies for extended instruction
                // Modified to permit use of compact expansion if address fits in 15 bits. 
                val instLength = instruction.instructionLength
                this.textAddress += (instLength)
                result.add(programStatement)
                return result
            }
        }
        return null
    }

    /**
     * Pre-process the token list for a statement by stripping off any label, if
     * either are present. Any label definition will be recorded in the symbol
     * table. NOTE: the TokenList parameter will be modified.
     */
    private fun stripLabels(tokens: TokenList) {
        // If there is a label, handle it here and strip it off.
        val thereWasLabel = this.parseAndRecordLabel(tokens)
        if (thereWasLabel) {
            tokens.remove(0) // Remove the IDENTIFIER.
            tokens.remove(0) // Remove the COLON, shifted to 0 by previous remove
        }
    }

    // Parse and record label, if there is one. Note the identifier and its colon
    // are
    // two separate tokens, since they may be separated by spaces in source code.
    private fun parseAndRecordLabel(tokens: TokenList): Boolean {
        if (tokens.size < 2) {
            return false
        } else {
            val token = tokens.get(0)
            if (tokenListBeginsWithLabel(tokens)) {
                if (token.type == TokenType.OPERATOR) {
                    // an instruction name was used as label (e.g. lw:), so change its token type
                    token.type = TokenType.IDENTIFIER
                }
                this.fileCurrentlyBeingAssembled!!.localSymbolTable!!.addSymbol(
                    token,
                    if (this.inDataSegment) this.dataAddress else this.textAddress,
                    this.inDataSegment, this.errors!!
                )
                return true
            } else {
                return false
            }
        }
    }

    /** This source code line is a directive, not a instruction. Let's carry it out. */
    private fun executeDirective(tokens: TokenList) {
        val token = tokens.get(0)
        val direct = Directive.matchDirective(token.text)
        if (Globals.debug) {
            LOGGER.debug("line {} is directive {}", token.sourceLine, direct)
        }
        when {
            direct == null -> this.errors!!.addTokenError(token, "Unrecognized directive: ${token.text}")

            direct == Directive.EQV -> Unit // Do nothing. This was vetted and processed during tokenizing.

            direct == Directive.MACRO -> {
                if (tokens.size < 2) {
                    val message = "\"${token.text}\" directive requires at least one argument."
                    this.errors!!.addTokenError(token, message)
                    return
                }
                val nextToken = tokens.get(1)
                if (nextToken.type != TokenType.IDENTIFIER) {
                    this.errors!!.addTokenError(nextToken, "Invalid Macro name \"${token.text}\"")
                    return
                }
                if (this.inMacroSegment) {
                    this.errors!!.addTokenError(token, "Nested macros are not allowed")
                    return
                }
                this.inMacroSegment = true
                val pool = this.fileCurrentlyBeingAssembled!!.localMacroPool!!
                pool.beginMacro(tokens.get(1))
                for (i in 2..<tokens.size) {
                    val arg = tokens.get(i)
                    if (arg.type == TokenType.RIGHT_PAREN
                        || arg.type == TokenType.LEFT_PAREN
                    ) {
                        continue
                    }
                    if (!Macro.tokenIsMacroParameter(arg.text, true)) {
                        this.errors!!.addTokenError(
                            arg, "Invalid macro argument '${arg.text}'"
                        )
                        return
                    }
                    pool.current!!.addArg(arg.text)
                }
            }

            direct == Directive.END_MACRO -> {
                if (tokens.size > 1) {
                    this.errors!!.addTokenError(
                        token, "invalid text after .END_MACRO"
                    )
                    return
                }
                if (!this.inMacroSegment) {
                    this.errors!!.addTokenError(
                        token, ".END_MACRO without .MACRO"
                    )
                    return
                }
                this.inMacroSegment = false
                this.fileCurrentlyBeingAssembled!!.localMacroPool!!.commitMacro(token)
            }

            this.inMacroSegment -> Unit // should not parse lines even directives in macro segment

            direct == Directive.DATA -> {
                this.inDataSegment = true
                this.autoAlign = true
                if (tokens.size > 1 && TokenType.isIntegerTokenType(tokens.get(1).type)) {
                    this.dataAddress = tokens.get(1).text.translateToInt()!!
                }
            }

            direct == Directive.TEXT -> {
                this.inDataSegment = false
                if (tokens.size > 1 && TokenType.isIntegerTokenType(tokens.get(1).type)) {
                    this.textAddress = tokens.get(1).text.translateToInt()!!
                }
            }

            direct == Directive.SECTION -> {
                if (tokens.size >= 2) {
                    val section = tokens.get(1)
                    if (section.type == TokenType.QUOTED_STRING || section.type == TokenType.IDENTIFIER) {
                        val str = section.text
                        if (str.startsWith(".data") || str.startsWith(".rodata") || str.startsWith(".sdata")) {
                            this.inDataSegment = true
                        } else if (str.startsWith(".text")) {
                            this.inDataSegment = false
                        } else {
                            val message =
                                "section name \"$str\" is ignored"
                            errors!!.addWarning(token, message)
                        }
                    } else {
                        this.errors!!.addTokenError(
                            token,
                            ".section must be followed by a section name ."
                        )
                    }
                } else {
                    errors!!.addWarning(
                        token,
                        ".section without arguments is ignored"
                    )
                }
            }

            direct == Directive.WORD ||
                direct == Directive.HALF ||
                direct == Directive.BYTE ||
                direct == Directive.FLOAT ||
                direct == Directive.DOUBLE ||
                direct == Directive.DWORD -> {
                this.dataDirective = direct
                if (this.passesDataSegmentCheck(token) && tokens.size > 1) {
                    this.storeNumeric(tokens, direct, this.errors!!)
                }
            }

            direct == Directive.ASCII ||
                direct == Directive.ASCIZ ||
                direct == Directive.STRING -> {
                this.dataDirective = direct
                if (this.passesDataSegmentCheck(token)) {
                    this.storeStrings(tokens, direct, this.errors!!)
                }
            }

            direct == Directive.ALIGN -> {
                if (tokens.size != 2) {
                    this.errors!!.addTokenError(token, "\"${token.text}\" requires one operand")
                    return
                }
                if (!TokenType.isIntegerTokenType(tokens.get(1).type)
                    || tokens[1].text.translateToInt()!! < 0
                ) {
                    this.errors!!.addTokenError(
                        token,
                        "\"${token.text}\" requires a non-negative integer"
                    )
                    return
                }
                val value = tokens[1].text.translateToInt()!!
                if (value < 2 && !this.inDataSegment) {
                    this.errors!!.add(
                        ErrorMessage.warning(
                            token.sourceProgram,
                            token.sourceLine,
                            token.startPos,
                            "Alignments less than 4 bytes are not supported in the text section."
                                + " The alignment has been rounded up to 4 bytes."
                        )
                    )
                    this.dataAddress = this.alignToBoundary(this.dataAddress, 4)
                } else if (value == 0) {
                    this.autoAlign = false
                } else {
                    this.dataAddress =
                        this.alignToBoundary(this.dataAddress, StrictMath.pow(2.0, value.toDouble()).toInt())
                }
            }

            direct == Directive.SPACE -> {
                // TODO: add a fill type option
                // .space 90, 1 should fill memory with 90 bytes with the values 1
                if (this.passesDataSegmentCheck(token)) {
                    if (tokens.size != 2) {
                        this.errors!!.addTokenError(token, "\"${token.text}\" requires one operand")
                        return
                    }
                    if (!TokenType.isIntegerTokenType(tokens.get(1).type)
                        || tokens.get(1).text.translateToInt()!! < 0
                    ) {
                        this.errors!!.addTokenError(
                            token,
                            "\"${token.text}\" requires a non-negative integer"
                        )
                        return
                    }
                    val value = tokens.get(1).text.translateToInt()!!
                    this.dataAddress += value
                }
            }

            direct == Directive.EXTERN -> {
                if (tokens.size != 3) {
                    this.errors!!.addTokenError(
                        token, "\"${token.text}\" directive requires two operands (label and size)."
                    )
                    return
                }
                if (!TokenType.isIntegerTokenType(tokens.get(2).type)
                    || tokens.get(2).text.translateToInt()!! < 0
                ) {
                    this.errors!!.addTokenError(
                        token,
                        "\"${token.text}\" requires a non-negative integer size"
                    )
                    return
                }
                val size = tokens.get(2).text.translateToInt()!!
                // If label already in global symtab, do nothing. If not, add it right now.
                if (Globals.GLOBAL_SYMBOL_TABLE.getAddress(tokens.get(1).text) == SymbolTable.NOT_FOUND) {
                    Globals.GLOBAL_SYMBOL_TABLE.addSymbol(
                        tokens.get(1), this.externAddress,
                        true, this.errors!!
                    )
                    this.externAddress += size
                }
            }

            direct == Directive.GLOBL ||
                direct == Directive.GLOBAL -> {
                if (tokens.size < 2) {
                    this.errors!!.addTokenError(
                        token,
                        "\"${token.text}\" directive requires at least one argument."
                    )
                    return
                }
                // SPIM limits .globl list to one label, why not extend it to a list?
                for (i in 1..<tokens.size) {
                    // Add it to a list of labels to be processed at the end of the
                    // pass. At that point, transfer matching symbol definitions from
                    // local symbol table to global symbol table.
                    val label = tokens.get(i)
                    if (label.type != TokenType.IDENTIFIER) {
                        this.errors!!.addTokenError(
                            token,
                            "\"${token.text}\" directive argument must be label."
                        )
                        return
                    }
                    this.globalDeclarationList!!.add(label)
                }
            }

            else -> this.errors!!.addTokenError(
                token,
                "Directive \"${token.text}\" recognized but not yet implemented."
            )
        }
    }

    /** Process the list of .globl labels, if any, declared and defined in this file.
     * We'll just move their symbol table entries from local symbol table to global
     * symbol table at the end of the first assembly pass. */
    private fun transferGlobals() {
        for (i in this.globalDeclarationList!!.indices) {
            val label = this.globalDeclarationList!!.get(i)
            val symtabEntry = this.fileCurrentlyBeingAssembled!!.localSymbolTable!!.getSymbol(
                label.text
            )
            if (symtabEntry == null) {
                this.errors!!.addTokenError(
                    label,
                    "Label \"${label.text}\" declared global but not defined."
                )
                // TODO: allow this case, but check later to see if all requested globals are
                // actually implemented in other files
                // GCC outputs assembly that uses this
            } else {
                if (Globals.GLOBAL_SYMBOL_TABLE.getAddress(label.text) != SymbolTable.NOT_FOUND) {
                    this.errors!!.addTokenError(
                        label, "Label \"${label.text}\" already defined as global in a different file."
                    )
                } else {
                    this.fileCurrentlyBeingAssembled!!.localSymbolTable!!.removeSymbol(label)
                    Globals.GLOBAL_SYMBOL_TABLE.addSymbol(
                        label, symtabEntry.address,
                        symtabEntry.isData, this.errors!!
                    )
                }
            }
        }
    }

    // This source code line, if syntactically correct, is a continuation of a
    // directive list begun on on previous line.
    private fun executeDirectiveContinuation(tokens: TokenList) {
        val direct = this.dataDirective
        if (direct == Directive.WORD || direct == Directive.HALF || direct == Directive.BYTE || direct == Directive.FLOAT || direct == Directive.DOUBLE || direct == Directive.DWORD) {
            if (!tokens.isEmpty()) {
                this.storeNumeric(tokens, direct, this.errors!!)
            }
        } else if (direct == Directive.ASCII || direct == Directive.ASCIZ || direct == Directive.STRING) {
            if (this.passesDataSegmentCheck(tokens.get(0))) {
                this.storeStrings(tokens, direct, this.errors!!)
            }
        }
    }

    // Given token, find the corresponding Instruction object. If token was not
    // recognized as OPERATOR, there is a problem.
    private fun matchInstruction(token: Token): List<Instruction>? {
        if (token.type != TokenType.OPERATOR) {
            if (token.sourceProgram!!.localMacroPool!!.matchesAnyMacroName(token.text)) {
                this.errors!!.addTokenError(
                    token,
                    "forward reference or invalid parameters for macro \"${token.text}\""
                )
            } else {
                this.errors!!.addTokenError(
                    token, "Expected operator, found \"${token.text}\""
                )
            }
            return null
        }
        val instructions = InstructionsRegistry.matchOperator(token.text)
        if (instructions.isEmpty()) {
            // This should NEVER happen...
            this.errors!!.addTokenError(
                token,
                "Internal Assembler error: \"${token.text}\" tokenized OPERATOR then not recognized"
            )
            return null
        }
        return instructions
    }

    /**
     * Processes the .word/.half/.byte/.float/.double directive.
     * Can also handle "directive continuations", e.g. second or subsequent line
     * of a multiline list, which does not contain the directive token. Just pass
     * the
     * current directive as argument.
     */
    private fun storeNumeric(tokens: TokenList, directive: Directive, errors: ErrorList) {
        var token = tokens.get(0)
        // A double-check; should have already been caught...removed ".word" exemption
        // 11/20/06
        assert(this.passesDataSegmentCheck(token))
        // Correctly handles case where this is a "directive continuation" line.
        var tokenStart = 0
        if (token.type == TokenType.DIRECTIVE) {
            tokenStart = 1
        }

        // Set byte length in memory of each number (e.g. WORD is 4, BYTE is 1, etc)
        val lengthInBytes = DataTypes.getLengthInBytes(directive)

        // Handle the "value : n" format, which replicates the value "n" times.
        if (tokens.size == 4 && tokens.get(2).type == TokenType.COLON) {
            val valueToken = tokens.get(1)
            val repetitionsToken = tokens.get(3)
            // Conditions for correctly-formed replication:
            // (integer directive AND integer value OR floating directive AND
            // (integer value OR floating value))
            // AND integer repetition value
            if (!(directive.isIntegerDirective)
                || !TokenType.isIntegerTokenType(repetitionsToken.type)
            ) {
                errors.addTokenError(
                    valueToken,
                    "malformed expression"
                )
                return
            }
            val repetitions = repetitionsToken.text.translateToInt()!!
            if (repetitions <= 0) {
                errors.addTokenError(
                    repetitionsToken,
                    "repetition factor must be positive"
                )
                return
            }
            if (this.inDataSegment) {
                if (this.autoAlign) {
                    this.dataAddress = this.alignToBoundary(this.dataAddress, lengthInBytes)
                }
                repeat(repetitions) {
                    if (directive.isIntegerDirective) {
                        this.storeInteger(valueToken, directive, errors)
                    } else {
                        this.storeRealNumber(valueToken, directive, errors)
                    }
                }
            }
            return
        }

        // if not in ".word w : n" format, must just be list of one or more values.
        for (i in tokenStart..<tokens.size) {
            token = tokens.get(i)
            if (directive.isIntegerDirective) {
                this.storeInteger(token, directive, errors)
            }
            if (directive.isFloatingDirective) {
                this.storeRealNumber(token, directive, errors)
            }
        }
    }

    /**
     * Store integer value given integer (word, half, byte) directive.
     * Called by storeNumeric()
     * NOTE: The token itself may be a label, in which case the correct action is
     * to store the address of that label (into however many bytes specified).
     */
    private fun storeInteger(
        token: Token,
        directive: Directive,
        errors: ErrorList
    ) {
        val lengthInBytes = DataTypes.getLengthInBytes(directive)
        if (TokenType.isIntegerTokenType(token.type)) {
            var value: Int
            val longValue: Long
            if (TokenType.INTEGER_64 == token.type) {
                longValue = token.text.translateToLong()!!
                value = longValue.toInt()
                if (directive != Directive.DWORD) {
                    val message = "value ${longValue.toHexStringWithPrefix()} " +
                        "is out-of-range and truncated to ${value.toHexStringWithPrefix()}"
                    errors.addWarning(token, message)
                }
            } else {
                value = token.text.translateToInt()!!
                longValue = value.toLong()
            }

            if (directive == Directive.DWORD) {
                this.writeToDataSegment(longValue.toInt(), 4, token, errors)
                this.writeToDataSegment((longValue shr 32).toInt(), 4, token, errors)
                return
            }

            val fullValue = value
            // If value is out of range for the directive, will simply truncate
            // the leading bits (includes sign bits). This is what SPIM does.
            // But will issue a warning (not error) which SPIM does not do.
            if (directive == Directive.BYTE) {
                value = value and 0x000000FF
            } else if (directive == Directive.HALF) {
                value = value and 0x0000FFFF
            }

            if (DataTypes.outOfRange(directive, fullValue)) {
                errors.addWarning(
                    token,
                    "value ${fullValue.toHexStringWithPrefix()} is out-of-range and truncated to ${value.toHexStringWithPrefix()}"
                )
            }
            if (this.inDataSegment) {
                this.writeToDataSegment(value, lengthInBytes, token, errors)
            } else {
                Globals.MEMORY_INSTANCE
                    .set(this.textAddress, value, lengthInBytes)
                    .onLeft {
                        errors.addTokenError(
                            token, "\"${this.textAddress}\" is not a valid text segment address"
                        )
                        return
                    }
                this.textAddress += lengthInBytes
            }
        } else if (token.type == TokenType.IDENTIFIER) {
            if (this.inDataSegment) {
                val value = this.fileCurrentlyBeingAssembled!!.localSymbolTable!!
                    .getAddressLocalOrGlobal(token.text)
                if (value == SymbolTable.NOT_FOUND) {
                    // Record value 0 for now, then set up backpatch entry
                    val dataAddress = this.writeToDataSegment(0, lengthInBytes, token, errors)
                    this.currentFileDataSegmentForwardReferenceList!!.add(dataAddress, lengthInBytes, token)
                } else {
                    // label already defined, so write its address
                    this.writeToDataSegment(value, lengthInBytes, token, errors)
                }
            }
            // Data segment check done previously, so this "else" will not be.
            else {
                errors.addTokenError(
                    token, "\"${token.text}\" label as directive operand not permitted in text segment"
                )
            }
        } else {
            errors.addTokenError(token, "\"${token.text}\" is not a valid integer constant or label")
        }
    }

    /**
     * Store real (fixed or floating point) value given floating (float, double)
     * directive.
     * Called by storeNumeric()
     */
    private fun storeRealNumber(
        token: Token,
        directive: Directive,
        errors: ErrorList
    ) {
        val lengthInBytes = DataTypes.getLengthInBytes(directive)
        val value: Double
        if (token.text == "Inf") {
            value = Float.POSITIVE_INFINITY.toDouble()
        } else if (token.text == "-Inf") {
            value = Float.NEGATIVE_INFINITY.toDouble()
        } else if (TokenType.isIntegerTokenType(token.type) || TokenType.isFloatingTokenType(token.type)) {
            try {
                value = token.text.toDouble()
            } catch (_: NumberFormatException) {
                errors.addTokenError(
                    token,
                    "\"${token.text}\" is not a valid floating point constant"
                )
                return
            }
            if (DataTypes.outOfRange(directive, value)) {
                errors.addTokenError(token, "\"${token.text}\" is an out-of-range value")
                return
            }
        } else {
            val message = "\"${token.text}\" is not a valid floating point constant"
            errors.addTokenError(token, message)
            return
        }

        // Value has been validated; let's store it.
        if (directive == Directive.FLOAT) {
            this.writeToDataSegment(value.toFloat().toRawBits(), lengthInBytes, token, errors)
        }
        if (directive == Directive.DOUBLE) {
            this.writeDoubleToDataSegment(value, token)
        }
    }

    /**
     * Use directive argument to distinguish between ASCII and ASCIZ. The
     * latter stores a terminating null byte. Can handle a list of one or more
     * strings on a single line.
     */
    private fun storeStrings(
        tokens: TokenList,
        direct: Directive,
        errors: ErrorList
    ) {
        // Correctly handles case where this is a "directive continuation" line.
        val isFirstDirective = tokens.first().type == TokenType.DIRECTIVE
        tokens
            .drop(if (isFirstDirective) 1 else 0)
            .forEach { token ->
                if (token.type != TokenType.QUOTED_STRING) {
                    errors.addTokenError(
                        token, "\"${token.text}\" is not a valid character string"
                    )
                } else {
                    val quote = token.text
                    var j = 1
                    while (j < quote.length - 1) {
                        var theChar = quote[j]
                        if (theChar == '\\') {
                            theChar = quote[++j]
                            when (theChar) {
                                'n' -> theChar = '\n'
                                't' -> theChar = '\t'
                                'r' -> theChar = '\r'
                                '\\', '"', '\'' -> {}
                                'b' -> theChar = '\b'
                                'f' -> theChar = '\u000c'
                                '0' -> theChar = '\u0000'
                                'u' -> {
                                    val codePoint = ""
                                    try {
                                        val codePoint = quote.substring(
                                            j + 1,
                                            j + 5
                                        ) // get the UTF-8 codepoint following the
                                        // unicode escape sequence
                                        theChar = Character.toChars(codePoint.toInt(16))[0] // converts the
                                        // codepoint to
                                        // single character
                                    } catch (_: StringIndexOutOfBoundsException) {
                                        val invalidCodePoint = quote.substring(j + 1)
                                        val message: String =
                                            "unicode escape \"\\u$invalidCodePoint\" is incomplete. " +
                                                "Only escapes with 4 digits are valid."
                                        errors.addTokenError(token, message)
                                    } catch (_: NumberFormatException) {
                                        errors.addTokenError(
                                            token,
                                            "illegal unicode escape: \"\\u$codePoint\""
                                        )
                                    }
                                    j = j + 4 // skip past the codepoint for next iteration
                                }
                            }
                        }
                        val bytesOfChar = theChar.toString().toByteArray(Charsets.UTF_8)
                        either {
                            for (b in bytesOfChar) {
                                Globals.MEMORY_INSTANCE.set(
                                    this@Assembler.dataAddress, b.toInt(),
                                    DataTypes.CHAR_SIZE
                                ).bind()
                                this@Assembler.dataAddress += DataTypes.CHAR_SIZE
                            }
                        }.onLeft {
                            this.errors!!.addTokenError(
                                token,
                                "\"${this.dataAddress}\" is not a valid data segment address"
                            )
                        }

                        j++
                    }
                    if (direct == Directive.ASCIZ || direct == Directive.STRING) {
                        Globals.MEMORY_INSTANCE.set(this.dataAddress, 0, DataTypes.CHAR_SIZE).onLeft {
                            this.errors!!.addTokenError(
                                token,
                                "\"${this.dataAddress}\" is not a valid data segment address"
                            )
                        }
                        this.dataAddress += DataTypes.CHAR_SIZE
                    }
                }
            }
    }

    /**
     * Simply check to see if we are in data segment. Generate error if not.
     */
    private fun passesDataSegmentCheck(token: Token): Boolean = if (!this.inDataSegment) {
        val message = "\"${token.text}\" directive cannot appear in text segment"
        this.errors!!.addTokenError(token, message)
        false
    } else {
        true
    }

    /**
     * Writes the given int value into current data segment address. Works for
     * all the integer types plus float (caller is responsible for doing
     * floatToIntBits).
     * Returns address at which the value was stored.
     */
    private fun writeToDataSegment(
        value: Int, lengthInBytes: Int, token: Token,
        errors: ErrorList
    ): Int {
        if (this.autoAlign) {
            this.dataAddress = this.alignToBoundary(this.dataAddress, lengthInBytes)
        }
        Globals.MEMORY_INSTANCE.set(this.dataAddress, value, lengthInBytes).onLeft {
            val message = "\"${this.dataAddress}\" is not a valid data segment address"
            errors.addTokenError(token, message)
            return this.dataAddress
        }
        val address = this.dataAddress
        this.dataAddress += lengthInBytes
        return address
    }

    /**
     * Writes the given double value into current data segment address. Works
     * only for DOUBLE floating
     * point values -- Memory class doesn't have method for writing 8 bytes, so
     * use setWord twice.
     */
    private fun writeDoubleToDataSegment(value: Double, token: Token) {
        val lengthInBytes = DataTypes.DOUBLE_SIZE
        if (this.autoAlign) {
            this.dataAddress = (this.alignToBoundary(this.dataAddress, lengthInBytes))
        }
        Globals.MEMORY_INSTANCE.setDouble(this.dataAddress, value).onLeft {
            this.errors!!.addTokenError(token, "\"${this.dataAddress}\" is not a valid data segment address")
        }
        this.dataAddress += lengthInBytes
    }

    /**
     * If address is multiple of byte boundary, returns address. Otherwise, returns
     * address
     * which is next higher multiple of the byte boundary. Used for aligning data
     * segment.
     * For instance if args are 6 and 4, returns 8 (next multiple of 4 higher than
     * 6).
     * NOTE: it will fix any symbol table entries for this address too. See else
     * part.
     */
    private fun alignToBoundary(address: Int, byteBoundary: Int): Int {
        val remainder = address % byteBoundary
        if (remainder == 0) {
            return address
        } else {
            val alignedAddress = address + byteBoundary - remainder
            this.fileCurrentlyBeingAssembled!!.localSymbolTable!!.fixSymbolTableAddress(
                address,
                alignedAddress
            )
            return alignedAddress
        }
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(Assembler::class.java)

        /**
         * Will check for duplicate text addresses, which can happen inadvertently when
         * using
         * operand on .text directive. Will generate error message for each one that
         * occurs.
         */
        private fun catchDuplicateAddresses(
            instructions: List<ProgramStatement>,
            errors: ErrorList
        ) {
            for (i in 0..<instructions.size - 1) {
                val ps1 = instructions[i]
                val ps2 = instructions[i + 1]
                if (ps1.address == ps2.address) {
                    val formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(
                        ps2.address,
                        if (Globals.BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX)) 16 else 10
                    )
                    val directiveText = if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(ps2.address))
                        ".text"
                    else
                        ".ktext"
                    val message = "Duplicate text segment address: $formattedAddress " +
                        "already occupied by ${ps1.sourceLine!!.program.file} " +
                        "line ${ps1.sourceLine.lineNumber} " +
                        "(caused by use of $directiveText operand)"
                    errors.add(
                        ErrorMessage.error(
                            ps2.sourceProgram,
                            ps2.sourceLine!!.lineNumber,
                            0,
                            message
                        )
                    )
                }
            }
        }

        private fun detectLabels(tokens: TokenList, current: Macro) {
            if (tokenListBeginsWithLabel(tokens)) {
                current.addLabel(tokens.get(0).text)
            }
        }

        // Pre-process the token list for a statement by stripping off any comment.
        // NOTE: the ArrayList parameter is not modified; a new one is cloned and
        // returned.
        private fun stripComment(tokenList: TokenList): TokenList {
            if (tokenList.isEmpty()) {
                return tokenList
            }
            val tokens = tokenList.clone() as TokenList
            // If there is a comment, strip it off.
            val last = tokens.size - 1
            if (tokens.get(last).type == TokenType.COMMENT) {
                tokens.remove(last)
            }
            return tokens
        }

        private fun tokenListBeginsWithLabel(tokens: TokenList): Boolean = if (tokens.size < 2) false
        else (tokens[0].type == TokenType.IDENTIFIER ||
            tokens[0].type == TokenType.OPERATOR)
            && tokens[1].type == TokenType.COLON

        fun assemble(
            tokenizedProgramFiles: List<RISCVProgram>,
            extendedAssemblerEnabled: Boolean,
            warningsAreErrors: Boolean
        ): Either<AssemblyError, Pair<List<ProgramStatement>, ErrorList>> = either {
            val assembler = Assembler()
            val machineList = assembler.assembleImpl(
                tokenizedProgramFiles,
                extendedAssemblerEnabled,
                warningsAreErrors
            ).bind()
            Pair(machineList, assembler.errors!!)
        }
    }
}
