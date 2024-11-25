package rars;

import org.jetbrains.annotations.Nullable;
import rars.assembler.*;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.hardware.RegisterFile;
import rars.simulator.BackStepper;
import rars.simulator.Simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

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
public class RISCVprogram {

    private String filename;
    private ArrayList<String> sourceList;
    private ArrayList<TokenList> tokenList;
    private ArrayList<ProgramStatement> parsedList;
    private ArrayList<ProgramStatement> machineList;
    private BackStepper backStepper;
    private SymbolTable localSymbolTable;
    private MacroPool macroPool;
    private ArrayList<SourceLine> sourceLineList;
    private Tokenizer tokenizer;

    /**
     * Simulates execution of the program (in this thread). Program must have
     * already been assembled.
     * Begins simulation at current program counter address and continues until
     * stopped,
     * paused, maximum steps exceeded, or exception occurs.
     *
     * @param maxSteps the maximum maximum number of steps to simulate.
     * @return reason for the interruption of the program
     * @throws SimulationException Will throw exception if errors occurred while
     *                             simulating.
     */
    public static Simulator.Reason simulate(final int maxSteps) throws SimulationException {
        final Simulator sim = Simulator.getInstance();
        return sim.simulate(RegisterFile.getProgramCounter(), maxSteps, null);
    }

    /**
     * Simulates execution of the program (in a new thread). Program must have
     * already been assembled.
     * Begins simulation at current program counter address and continues until
     * stopped,
     * paused, maximum steps exceeded, or exception occurs.
     *
     * @param maxSteps    maximum number of instruction executions. Default -1 means
     *                    no maximum.
     * @param breakPoints int array of breakpoints (PC addresses). Can be null.
     */
    public static void startSimulation(final int maxSteps, final int[] breakPoints) {
        final Simulator sim = Simulator.getInstance();
        sim.startSimulation(RegisterFile.getProgramCounter(), maxSteps, breakPoints);
    }

    /**
     * Produces list of source statements that comprise the program.
     *
     * @return ArrayList of String. Each String is one line of RISCV source code.
     */
    public ArrayList<String> getSourceList() {
        return this.sourceList;
    }

    /**
     * Retrieve list of source statements that comprise the program.
     *
     * @return ArrayList of SourceLine.
     * Each SourceLine represents one line of RISCV source code
     */
    public ArrayList<SourceLine> getSourceLineList() {
        return this.sourceLineList;
    }

    /**
     * Set list of source statements that comprise the program.
     *
     * @param sourceLineList ArrayList of SourceLine.
     *                       Each SourceLine represents one line of RISCV source
     *                       code.
     */
    public void setSourceLineList(final ArrayList<SourceLine> sourceLineList) {
        this.sourceLineList = sourceLineList;
        this.sourceList = new ArrayList<>();
        for (final SourceLine sl : sourceLineList) {
            this.sourceList.add(sl.source());
        }
    }

    /**
     * Produces name of associated source code file.
     *
     * @return File name as String.
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Produces list of tokens that comprise the program.
     *
     * @return ArrayList of TokenList. Each TokenList is list of tokens generated by
     * corresponding line of RISCV source code.
     * @see TokenList
     */
    public ArrayList<TokenList> getTokenList() {
        return this.tokenList;
    }

    /**
     * Retrieves Tokenizer for this program
     *
     * @return Tokenizer
     */
    public Tokenizer getTokenizer() {
        return this.tokenizer;
    }

    /**
     * Produces new empty list to hold parsed source code statements.
     *
     * @return ArrayList of ProgramStatement. Each ProgramStatement represents a
     * parsed RISCV statement.
     * @see ProgramStatement
     */
    public ArrayList<ProgramStatement> createParsedList() {
        this.parsedList = new ArrayList<>();
        return this.parsedList;
    }

    /**
     * Produces existing list of parsed source code statements.
     *
     * @return ArrayList of ProgramStatement. Each ProgramStatement represents a
     * parsed RISCV statement.
     * @see ProgramStatement
     */
    public ArrayList<ProgramStatement> getParsedList() {
        return this.parsedList;
    }

    /**
     * Produces list of machine statements that are assembled from the program.
     *
     * @return ArrayList of ProgramStatement. Each ProgramStatement represents an
     * assembled
     * basic RISCV instruction.
     * @see ProgramStatement
     */
    public ArrayList<ProgramStatement> getMachineList() {
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
     * Returns status of BackStepper associated with this program.
     *
     * @return true if enabled, false if disabled or non-existant.
     */
    public boolean backSteppingEnabled() {
        return (this.backStepper != null && this.backStepper.enabled());
    }

    /**
     * Produces specified line of RISCV source program.
     *
     * @param i Line number of RISCV source program to get. Line 1 is first line.
     * @return Returns specified line of RISCV source. If outside the line range,
     * it returns null. Line 1 is first line.
     */
    public @Nullable String getSourceLine(final int i) {
        if ((i >= 1) && (i <= this.sourceList.size()))
            return this.sourceList.get(i - 1);
        else
            return null;
    }

    /**
     * Reads RISCV source code from a string into structure.
     *
     * @param source String containing the RISCV source code.
     */
    public void fromString(final String source) {
        this.filename = source;
        this.sourceList = new ArrayList<>(Arrays.asList(source.split("\n")));
    }

    /**
     * Reads RISCV source code from file into structure. Will always read from file.
     * It is GUI responsibility to assure that source edits are written to file
     * when user selects compile or run/step options.
     *
     * @param file String containing name of RISCV source code file.
     * @throws AssemblyException Will throw exception if there is any problem
     *                           reading the file.
     */
    public void readSource(final String file) throws AssemblyException {
        this.filename = file;
        this.sourceList = new ArrayList<>();
        final ErrorList errors;
        final BufferedReader inputFile;
        String line;
        try {
            inputFile = new BufferedReader(new FileReader(file));
            line = inputFile.readLine();
            while (line != null) {
                this.sourceList.add(line);
                line = inputFile.readLine();
            }
        } catch (final Exception e) {
            errors = new ErrorList();
            errors.add(new ErrorMessage(null, 0, 0, e.toString()));
            throw new AssemblyException(errors);
        }
    }

    /**
     * Tokenizes the RISCV source program. Program must have already been read from
     * file.
     *
     * @throws AssemblyException Will throw exception if errors occurred while
     *                           tokenizing.
     */
    public void tokenize() throws AssemblyException {
        this.tokenizer = new Tokenizer();
        this.tokenList = this.tokenizer.tokenize(this);
        this.localSymbolTable = new SymbolTable(this.filename); // prepare for assembly
    }

    /**
     * Prepares the given list of files for assembly. This involves
     * reading and tokenizing all the source files. There may be only one.
     *
     * @param filenames        ArrayList containing the source file name(s) in no
     *                         particular order
     * @param leadFilename     String containing name of source file that needs to
     *                         go first and
     *                         will be represented by "this" RISCVprogram object.
     * @param exceptionHandler String containing name of source file containing
     *                         exception
     *                         handler. This will be assembled first, even ahead of
     *                         leadFilename, to allow it to
     *                         include "startup" instructions loaded beginning at
     *                         0x00400000. Specify null or
     *                         empty String to indicate there is no such designated
     *                         exception handler.
     * @return ArrayList containing one RISCVprogram object for each file to
     * assemble.
     * objects for any additional files (send ArrayList to assembler)
     * @throws AssemblyException Will throw exception if errors occurred while
     *                           reading or tokenizing.
     */
    public ArrayList<RISCVprogram> prepareFilesForAssembly(final ArrayList<String> filenames, final String leadFilename,
                                                           final String exceptionHandler) throws AssemblyException {
        final ArrayList<RISCVprogram> programsToAssemble = new ArrayList<>();
        int leadFilePosition = 0;
        if (exceptionHandler != null && !exceptionHandler.isEmpty()) {
            filenames.addFirst(exceptionHandler);
            leadFilePosition = 1;
        }
        for (final String filename : filenames) {
            final RISCVprogram preparee = (filename.equals(leadFilename)) ? this : new RISCVprogram();
            preparee.readSource(filename);
            preparee.tokenize();
            // I want "this" RISCVprogram to be the first in the list...except for exception
            // handler
            if (preparee == this && !programsToAssemble.isEmpty()) {
                programsToAssemble.add(leadFilePosition, preparee);
            } else {
                programsToAssemble.add(preparee);
            }
        }
        return programsToAssemble;
    }

    /**
     * Assembles the RISCV source program. All files comprising the program must
     * have
     * already been tokenized.
     *
     * @param programsToAssemble       ArrayList of RISCVprogram objects, each
     *                                 representing a tokenized source file.
     * @param extendedAssemblerEnabled A boolean second - true means extended
     *                                 (pseudo) instructions
     *                                 are permitted in source code and false means
     *                                 they are to be flagged as errors
     * @param warningsAreErrors        A boolean second - true means assembler
     *                                 warnings will be considered errors and
     *                                 terminate
     *                                 the assemble; false means the assembler will
     *                                 produce warning message but otherwise ignore
     *                                 warnings.
     * @return ErrorList containing nothing or only warnings (otherwise would have
     * thrown exception).
     * @throws AssemblyException Will throw exception if errors occurred while
     *                           assembling.
     */
    public ErrorList assemble(final ArrayList<RISCVprogram> programsToAssemble, final boolean extendedAssemblerEnabled,
                              final boolean warningsAreErrors) throws AssemblyException {
        this.backStepper = null;
        final Assembler asm = new Assembler();
        this.machineList = asm.assemble(programsToAssemble, extendedAssemblerEnabled, warningsAreErrors);
        this.backStepper = new BackStepper();
        return asm.getErrorList();
    }

    /**
     * Instantiates a new {@link MacroPool} and sends reference of this
     * {@link RISCVprogram} to it
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
     * @param macroPool reference to MacroPool
     * @author M.H.Sekhavat &lt;sekhavat17@gmail.com&gt;
     */
    public void setLocalMacroPool(final MacroPool macroPool) {
        this.macroPool = macroPool;
    }

} // RISCVprogram
