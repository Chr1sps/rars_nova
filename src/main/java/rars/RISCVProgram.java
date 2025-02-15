package rars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.*;
import rars.exceptions.AssemblyException;
import rars.simulator.BackStepper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Internal representations of the program. Connects source, tokens and machine
 * code. Having
 * all these structures available facilitates construction of good messages,
 * debugging, and easy simulation.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class RISCVProgram {

    private @Nullable File file;
    private List<@NotNull String> sourceList;
    private List<@NotNull TokenList> tokenList;
    private List<@NotNull ProgramStatement> parsedList;
    private List<@NotNull ProgramStatement> machineList;
    private BackStepper backStepper;
    private SymbolTable localSymbolTable;
    private MacroPool macroPool;
    private List<@NotNull SourceLine> sourceLineList;

    /**
     * Produces list of source statements that comprise the program.
     *
     * @return ArrayList of String. Each String is one line of RISCV source code.
     */
    public List<@NotNull String> getSourceList() {
        return this.sourceList;
    }

    /**
     * Retrieve list of source statements that comprise the program.
     *
     * @return ArrayList of SourceLine.
     * Each SourceLine represents one line of RISCV source code
     */
    public List<@NotNull SourceLine> getSourceLineList() {
        return this.sourceLineList;
    }

    /**
     * Set list of source statements that comprise the program.
     *
     * @param sourceLineList
     *     ArrayList of SourceLine.
     *     Each SourceLine represents one line of RISCV source
     *     code.
     */
    public void setSourceLineList(final @NotNull List<@NotNull SourceLine> sourceLineList) {
        this.sourceLineList = sourceLineList;
        this.sourceList = sourceLineList.stream().map(SourceLine::source).toList();
    }

    public @Nullable File getFile() {
        return this.file;
    }

    /**
     * Produces list of tokens that comprise the program.
     *
     * @return ArrayList of TokenList. Each TokenList is list of tokens generated by
     * corresponding line of RISCV source code.
     * @see TokenList
     */
    public List<@NotNull TokenList> getTokenList() {
        return this.tokenList;
    }

    /**
     * Produces existing list of parsed source code statements.
     *
     * @return ArrayList of ProgramStatement. Each ProgramStatement represents a
     * parsed RISCV statement.
     * @see ProgramStatement
     */
    public List<@NotNull ProgramStatement> getParsedList() {
        return this.parsedList;
    }

    public void setParsedList(final @NotNull List<@NotNull ProgramStatement> parsedList) {
        this.parsedList = parsedList;
    }

    /**
     * Produces list of machine statements that are assembled from the program.
     *
     * @return ArrayList of ProgramStatement. Each ProgramStatement represents an
     * assembled
     * basic RISCV instruction.
     * @see ProgramStatement
     */
    public @NotNull List<@NotNull ProgramStatement> getMachineList() {
        return this.machineList;
    }

    /**
     * Returns BackStepper associated with this program. It is created upon
     * successful assembly.
     *
     * @return BackStepper object, null if there is none.
     */
    public BackStepper getBackStepper() {
        return this.backStepper;
    }

    /**
     * Returns SymbolTable associated with this program. It is created at assembly
     * time,
     * and stores local labels (those not declared using .globl directive).
     *
     * @return a {@link SymbolTable} object
     */
    public SymbolTable getLocalSymbolTable() {
        return this.localSymbolTable;
    }

    /**
     * Produces specified line of RISCV source program.
     *
     * @param i
     *     Line number of RISCV source program to get. Line 1 is first line.
     * @return Returns specified line of RISCV source. If outside the line range,
     * it returns null. Line 1 is first line.
     */
    public @Nullable String getSourceLine(final int i) {
        if ((i >= 1) && (i <= this.sourceList.size())) {
            return this.sourceList.get(i - 1);
        } else {
            return null;
        }
    }

    /**
     * Reads RISCV source code from a string into structure.
     *
     * @param source
     *     String containing the RISCV source code.
     */
    public void fromString(final @NotNull String source) {
        this.file = null;
        this.sourceList = Arrays.asList(source.split("\n"));
    }

    /**
     * Reads RISCV source code from file into structure. Will always read from file.
     * It is GUI responsibility to assure that source edits are written to file
     * when user selects compile or run/step options.
     *
     * @param file
     *     String containing name of RISCV source code file.
     * @throws AssemblyException
     *     Will throw exception if there is any problem
     *     reading the file.
     */
    public void readSource(final @NotNull File file) throws AssemblyException {
        this.file = file;
        try {
            final BufferedReader inputFile = new BufferedReader(new FileReader(file));
            String line = inputFile.readLine();
            final var sourceList = new ArrayList<String>();
            while (line != null) {
                sourceList.add(line);
                line = inputFile.readLine();
            }
            this.sourceList = sourceList;
        } catch (final Exception e) {
            final ErrorList errors = new ErrorList();
            errors.add(ErrorMessage.error(null, 0, 0, e.toString()));
            throw new AssemblyException(errors);
        }
    }

    /**
     * Tokenizes the RISCV source program. Program must have already been read from
     * file.
     *
     * @throws AssemblyException
     *     Will throw exception if errors occurred while
     *     tokenizing.
     */
    public void tokenize() throws AssemblyException {
        this.tokenList = Tokenizer.tokenize(this);
        this.localSymbolTable = new SymbolTable(this.file, Globals.GLOBAL_SYMBOL_TABLE); // prepare for assembly
    }

    /**
     * Prepares the given list of files for assembly. This involves
     * reading and tokenizing all the source files. There may be only one.
     *
     * @param files
     *     ArrayList containing the source file name(s) in no
     *     particular order
     * @param leadFile
     *     String containing name of source file that needs to
     *     go first and
     *     will be represented by "this" RISCVprogram object.
     * @param exceptionHandler
     *     String containing name of source file containing
     *     exception
     *     handler. This will be assembled first, even ahead of
     *     leadFilename, to allow it to
     *     include "startup" instructions loaded beginning at
     *     0x00400000. Specify null or
     *     empty String to indicate there is no such designated
     *     exception handler.
     * @return ArrayList containing one RISCVprogram object for each file to
     * assemble.
     * objects for any additional files (send ArrayList to assembler)
     * @throws AssemblyException
     *     Will throw exception if errors occurred while
     *     reading or tokenizing.
     */
    public @NotNull List<@NotNull RISCVProgram> prepareFilesForAssembly(
        final @NotNull List<@NotNull File> files,
        final @NotNull File leadFile,
        final @Nullable File exceptionHandler
    ) throws AssemblyException {
        final var programsToAssemble = new ArrayList<RISCVProgram>();
        final int leadFilePosition = exceptionHandler == null ? 0 : 1;
        for (final var file : files) {
            final var prepareeProgram = (file.equals(leadFile)) ? this : new RISCVProgram();
            prepareeProgram.readSource(file);
            prepareeProgram.tokenize();
            // I want "this" RISCVprogram to be the first in the list...except for exception
            // handler
            if (prepareeProgram == this && !programsToAssemble.isEmpty()) {
                programsToAssemble.add(leadFilePosition, prepareeProgram);
            } else {
                programsToAssemble.add(prepareeProgram);
            }
        }
        return programsToAssemble;
    }

    /**
     * Assembles the RISCV source program. All files comprising the program must
     * have
     * already been tokenized.
     *
     * @param programsToAssemble
     *     ArrayList of RISCVprogram objects, each
     *     representing a tokenized source file.
     * @param extendedAssemblerEnabled
     *     A boolean value - true means extended
     *     (usePseudoInstructions) instructions
     *     are permitted in source code and false means
     *     they are to be flagged as errors
     * @param warningsAreErrors
     *     A boolean value - true means assembler
     *     warnings will be considered errors and
     *     terminate
     *     the assemble; false means the assembler will
     *     produce warning message but otherwise ignore
     *     warnings.
     * @return ErrorList containing nothing or only warnings (otherwise would have
     * thrown exception).
     * @throws AssemblyException
     *     Will throw exception if errors occurred while
     *     assembling.
     */
    public @NotNull ErrorList assemble(
        final @NotNull List<@NotNull RISCVProgram> programsToAssemble,
        final boolean extendedAssemblerEnabled,
        final boolean warningsAreErrors
    ) throws AssemblyException {
        this.backStepper = null;
        final var assemblyResult = Assembler.assemble(
            programsToAssemble,
            extendedAssemblerEnabled,
            warningsAreErrors
        );
        this.machineList = assemblyResult.first();
        this.backStepper = new BackStepper();
        return assemblyResult.second();
    }

    /**
     * Instantiates a new {@link MacroPool} and sends reference of this
     * {@link RISCVProgram} to it
     *
     * @return instatiated MacroPool
     * @author M.H.Sekhavat &lt;sekhavat17@gmail.com&gt;
     */
    public MacroPool createMacroPool() {
        this.macroPool = new MacroPool(this);
        return this.macroPool;
    }

    /**
     * Gets local macro pool {@link MacroPool} for this program
     *
     * @return MacroPool
     * @author M.H.Sekhavat &lt;sekhavat17@gmail.com&gt;
     */
    public MacroPool getLocalMacroPool() {
        return this.macroPool;
    }

    /**
     * Sets local macro pool {@link MacroPool} for this program
     *
     * @param macroPool
     *     reference to MacroPool
     * @author M.H.Sekhavat &lt;sekhavat17@gmail.com&gt;
     */
    public void setLocalMacroPool(final MacroPool macroPool) {
        this.macroPool = macroPool;
    }

} // RISCVprogram
