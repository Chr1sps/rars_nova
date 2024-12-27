package rars;

import com.formdev.flatlaf.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.api.Options;
import rars.api.Program;
import rars.exceptions.AddressErrorException;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.Instructions;
import rars.riscv.dump.DumpFormat;
import rars.riscv.dump.DumpFormatLoader;
import rars.riscv.hardware.*;
import rars.settings.BoolSetting;
import rars.simulator.Simulator;
import rars.util.Binary;
import rars.util.FilenameFinder;
import rars.util.MemoryDump;
import rars.util.Pair;
import rars.venus.VenusUI;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static rars.settings.Settings.BOOL_SETTINGS;

/*
Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

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
 * Launch the application
 *
 * @author Pete Sanderson
 * @version December 2009
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Main {

    private static final String rangeSeparator = "-";
    private static final int memoryWordsPerLine = 4; // display 4 memory words, tab separated, per line
    private static final int DECIMAL = 0; // memory and register display format
    private static final int HEXADECIMAL = 1;// memory and register display format
    private static final int ASCII = 2;// memory and register display format

    private final Options options;
    private final ArrayList<String> registerDisplayList;
    private final ArrayList<String> memoryDisplayList;
    private final ArrayList<String> filenameList;
    private boolean gui;
    private boolean simulate;
    private boolean rv64;
    private int displayFormat;
    private boolean verbose; // display register name or address along with contents
    private boolean assembleProject; // assemble only the given file or all files in its directory
    private boolean countInstructions; // Whether to count and report number of instructions executed
    private PrintStream out; // stream for display of command line output
    private ArrayList<String[]> dumpTriples = null; // each element holds 3 arguments for dump option
    private ArrayList<String> programArgumentList; // optional program args for program (becomes argc, argv)
    private int assembleErrorExitCode; // RARS command exit code to return if assemble error occurs
    private int simulateErrorExitCode;// RARS command exit code to return if simulation error occurs

    private Main(final String[] args) {
        this.options = new Options();
        this.gui = args.length == 0;
        this.simulate = true;
        this.displayFormat = Main.HEXADECIMAL;
        this.verbose = true;
        this.assembleProject = false;
        this.countInstructions = false;
        final int instructionCount = 0;
        this.assembleErrorExitCode = 0;
        this.simulateErrorExitCode = 0;
        this.registerDisplayList = new ArrayList<>();
        this.memoryDisplayList = new ArrayList<>();
        this.filenameList = new ArrayList<>();
        CurrentMemoryConfiguration.set(MemoryConfiguration.DEFAULT);
        this.out = System.out;

        if (!this.parseCommandArgs(args)) {
            System.exit(Globals.exitCode);
        }

        if (this.gui) {
            this.launchIDE();
        } else { // running from command line.
            // assure command mode works in headless environment (generates exception if
            // not)
            System.setProperty("java.awt.headless", "true");

            this.dumpSegments(this.runCommand());
            System.exit(Globals.exitCode);
        }
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     */
    public static void main(final String[] args) {
        new Main(args);
    }

    private static String[] checkMemoryAddressRange(final String arg) throws NumberFormatException {
        String[] memoryRange = null;
        if (arg.indexOf(Main.rangeSeparator) > 0 &&
                arg.indexOf(Main.rangeSeparator) < arg.length() - 1) {
            // assume correct format, two numbers separated by -, no embedded spaces.
            // If that doesn't work it is invalid.
            memoryRange = new String[2];
            memoryRange[0] = arg.substring(0, arg.indexOf(Main.rangeSeparator));
            memoryRange[1] = arg.substring(arg.indexOf(Main.rangeSeparator) + 1);
            // NOTE: I will use homegrown decoder, because Integer.decode will throw
            // exception on address higher than 0x7FFFFFFF (e.g. sign bit is 1).
            if (Binary.stringToInt(memoryRange[0]) > Binary.stringToInt(memoryRange[1]) ||
                    !Memory.wordAligned(Binary.stringToInt(memoryRange[0])) ||
                    !Memory.wordAligned(Binary.stringToInt(memoryRange[1]))) {
                throw new NumberFormatException();
            }
        }
        return memoryRange;
    }

    /// //////////////////////////////////////////////////////////
    // Perform any specified dump operations. See "dump" option.
    //
    private void displayAllPostMortem(final Program program) {
        this.displayMiscellaneousPostMortem(program);
        this.displayRegistersPostMortem(program);
        this.displayMemoryPostMortem(program.getMemory());
    }

    /// //////////////////////////////////////////////////////////////
    // There are no command arguments, so run in interactive mode by
    // launching the GUI-fronted integrated development environment.
    private void dumpSegments(final Program program) {
        if (this.dumpTriples == null || program == null) {
            return;
        }

        for (final String[] triple : this.dumpTriples) {
            final File file = new File(triple[2]);
            Pair<Integer, Integer> segInfo = MemoryDump.getSegmentBounds(triple[0]);
            // If not segment name, see if it is address range instead. DPS 14-July-2008
            if (segInfo == null) {
                try {
                    final String[] memoryRange = Main.checkMemoryAddressRange(triple[0]);
                    segInfo = new Pair<>(Binary.stringToInt(memoryRange[0]), Binary.stringToInt(memoryRange[1]));
                } catch (final NumberFormatException |
                               NullPointerException ignored) {
                }
            }
            if (segInfo == null) {
                this.out.println("Error while attempting to save dump, segment/address-range " + triple[0] + " is " +
                        "invalid!");
                continue;
            }
            final DumpFormat format = DumpFormatLoader.findDumpFormatGivenCommandDescriptor(triple[1]);
            if (format == null) {
                this.out.println("Error while attempting to save dump, format " + triple[1] + " was not found!");
                continue;
            }
            try {
                final int highAddress = program.getMemory().getAddressOfFirstNull(segInfo.first(), segInfo.second())
                        - Memory.WORD_LENGTH_BYTES;
                if (highAddress < segInfo.first()) {
                    this.out.println("This segment has not been written to, there is nothing to dump.");
                    continue;
                }
                format.dumpMemoryRange(file, segInfo.first(), highAddress, program.getMemory());
            } catch (final FileNotFoundException e) {
                this.out.println("Error while attempting to save dump, file " + file + " was not found!");
            } catch (final AddressErrorException e) {
                this.out.println("Error while attempting to save dump, file " + file + "!  Could not access address: "
                        + e.address + "!");
            } catch (final IOException e) {
                this.out.println("Error while attempting to save dump, file " + file + "!  Disk IO failed!");
            }
        }
    }

    /// ///////////////////////////////////////////////////////////////////
    // Parse command line arguments. The initial parsing has already been
    // done, since each space-separated argument is already in a String array
    // element. Here, we check for validity, set switch variables as appropriate
    // and build data structures. For help option (h), display the help.
    // Returns true if command args parse OK, false otherwise.
    private void launchIDE() {
        System.setProperty("apple.laf.useScreenMenuBar", "true"); // Puts RARS menu
        // on Mac OS menu bar
        if (SystemInfo.isLinux) {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "RARS Nova");
            System.setProperty("apple.awt.application.appearance", "system");
        }
        SwingUtilities.invokeLater(
                () -> {
                    // Turn off metal's use of bold fonts
                    // UIManager.put("swing.boldMetal", Boolean.FALSE);
                    new VenusUI("RARS " + Globals.version, this.filenameList);
                });
    }

    /// ///////////////////////////////////////////////////////////////////
    // Carry out the rars command: assemble then optionally run
    // Returns false if no simulation (run) occurs, true otherwise.
    private boolean parseCommandArgs(final String @NotNull [] args) {
        final String noCopyrightSwitch = "nc";
        final String displayMessagesToErrSwitch = "me";
        boolean argsOK = true;
        boolean inProgramArgumentList = false;
        this.programArgumentList = null;
        if (args.length == 0) {
            return true; // should not get here...
        }
        // If the option to display RARS messages to standard erro is used,
        // it must be processed before any others (since messages may be
        // generated during option parsing).
        this.processDisplayMessagesToErrSwitch(args, displayMessagesToErrSwitch);
        this.displayCopyright(args, noCopyrightSwitch); // ..or not..
        if (args.length == 1 && args[0].equals("h")) {
            this.displayHelp();
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            // We have seen "pa" switch, so all remaining args are program args
            // that will become "argc" and "argv" for the program.
            if (inProgramArgumentList) {
                if (this.programArgumentList == null) {
                    this.programArgumentList = new ArrayList<>();
                }
                this.programArgumentList.add(args[i]);
                continue;
            }
            // Once we hit "pa", all remaining command args are assumed
            // to be program arguments.
            if (args[i].equalsIgnoreCase("pa")) {
                inProgramArgumentList = true;
                continue;
            }
            // messages-to-standard-error switch already processed, so ignore.
            if (args[i].equalsIgnoreCase(displayMessagesToErrSwitch)) {
                continue;
            }
            // no-copyright switch already processed, so ignore.
            if (args[i].equalsIgnoreCase(noCopyrightSwitch)) {
                continue;
            }
            if (args[i].equalsIgnoreCase("dump")) {
                if (args.length <= (i + 3)) {
                    this.out.println("Dump command line argument requires a segment, format and file name.");
                    argsOK = false;
                } else {
                    if (this.dumpTriples == null) {
                        this.dumpTriples = new ArrayList<>();
                    }
                    this.dumpTriples.add(new String[]{args[++i], args[++i], args[++i]});
                    // simulate = false;
                }
                continue;
            }
            if (args[i].equalsIgnoreCase("mc")) {
                final String configName = args[++i];
                final var config = MemoryConfiguration.fromIdString(configName);
                if (config == null) {
                    this.out.println("Invalid memory configuration: " + configName);
                    argsOK = false;
                } else {
                    CurrentMemoryConfiguration.set(config);
                }
                continue;
            }
            // Set RARS exit code for assemble error
            if (args[i].toLowerCase().indexOf("ae") == 0) {
                final String s = args[i].substring(2);
                try {
                    this.assembleErrorExitCode = Integer.decode(s);
                    continue;
                } catch (final NumberFormatException nfe) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            // Set RARS exit code for simulate error
            if (args[i].toLowerCase().indexOf("se") == 0) {
                final String s = args[i].substring(2);
                try {
                    this.simulateErrorExitCode = Integer.decode(s);
                    continue;
                } catch (final NumberFormatException nfe) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            if (args[i].equalsIgnoreCase("d")) {
                Globals.debug = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("a")) {
                this.simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("ad") ||
                    args[i].equalsIgnoreCase("da")) {
                Globals.debug = true;
                this.simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("p")) {
                this.assembleProject = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("dec")) {
                this.displayFormat = Main.DECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("g")) {
                this.gui = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("hex")) {
                this.displayFormat = Main.HEXADECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("ascii")) {
                this.displayFormat = Main.ASCII;
                continue;
            }
            if (args[i].equalsIgnoreCase("b")) {
                this.verbose = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("np") || args[i].equalsIgnoreCase("ne")) {
                this.options.pseudo = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("we")) { // added 14-July-2008 DPS
                this.options.warningsAreErrors = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("sm")) { // added 17-Dec-2009 DPS
                this.options.startAtMain = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("smc")) { // added 5-Jul-2013 DPS
                this.options.selfModifyingCode = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("rv64")) {
                this.rv64 = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("ic")) { // added 19-Jul-2012 DPS
                this.countInstructions = true;
                continue;
            }

            if (new File(args[i]).exists()) { // is it a file name?
                this.filenameList.add(args[i]);
                continue;
            }

            if (args[i].indexOf("x") == 0) {
                if (RegisterFile.getRegister(args[i]) == null &&
                        FloatingPointRegisterFile.getRegister(args[i]) == null) {
                    this.out.println("Invalid Register Name: " + args[i]);
                } else {
                    this.registerDisplayList.add(args[i]);
                }
                continue;
            }
            // check for register name w/o $. added 14-July-2008 DPS
            if (RegisterFile.getRegister(args[i]) != null ||
                    FloatingPointRegisterFile.getRegister(args[i]) != null ||
                    ControlAndStatusRegisterFile.getRegister(args[i]) != null) {
                this.registerDisplayList.add(args[i]);
                continue;
            }
            // Check for stand-alone integer, which is the max execution steps option
            try {
                this.options.maxSteps = Integer.decode(args[i]); // if we got here, it has to be OK
                continue;
            } catch (final NumberFormatException ignored) {
            }
            // Check for integer address range (m-n)
            try {
                final String[] memoryRange = Main.checkMemoryAddressRange(args[i]);
                this.memoryDisplayList.add(memoryRange[0]); // low end of range
                this.memoryDisplayList.add(memoryRange[1]); // high end of range
                continue;
            } catch (final NumberFormatException nfe) {
                this.out.println("Invalid/unaligned address or invalid range: " + args[i]);
                argsOK = false;
                continue;
            } catch (final NullPointerException npe) {
                // Do nothing. next statement will handle it
            }
            this.out.println("Invalid Command Argument: " + args[i]);
            argsOK = false;
        }
        return argsOK;
    }

    /// ///////////////////////////////////////////////////////////////////
    // Check for memory address subrange. Has to be two integers separated
    // by "-"; no embedded spaces. e.g. 0x00400000-0x00400010
    // If number is not multiple of 4, will be rounded up to next higher.
    private @Nullable Program runCommand() {
        if (this.filenameList.isEmpty()) {
            return null;
        }

        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, this.rv64);
        Instructions.RV64 = this.rv64;

        final File mainFile = new File(this.filenameList.getFirst()).getAbsoluteFile();// First file is "main" file
        final List<String> filesToAssemble;
        if (this.assembleProject) {
            filesToAssemble = FilenameFinder.getFilenameList(mainFile.getParent(), Globals.fileExtensions);
            if (this.filenameList.size() > 1) {
                // Using "p" project option PLUS listing more than one filename on command line.
                // Add the additional files, avoiding duplicates.
                this.filenameList.removeFirst(); // first one has already been processed
                final var moreFilesToAssemble = FilenameFinder.getFilenameList(this.filenameList,
                        FilenameFinder.MATCH_ALL_EXTENSIONS);
                // Remove any duplicates then merge the two lists.
                for (int index2 = 0; index2 < moreFilesToAssemble.size(); index2++) {
                    for (final String s : filesToAssemble) {
                        if (s.equals(moreFilesToAssemble.get(index2))) {
                            moreFilesToAssemble.remove(index2);
                            index2--; // adjust for left shift in moreFilesToAssemble...
                            break; // break out of inner loop...
                        }
                    }
                }
                filesToAssemble.addAll(moreFilesToAssemble);
            }
        } else {
            filesToAssemble = FilenameFinder.getFilenameList(this.filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
        }
        final Program program = new Program(this.options);
        try {
            if (Globals.debug) {
                this.out.println("---  TOKENIZING & ASSEMBLY BEGINS  ---");
            }
            final ErrorList warnings = program.assembleFiles(filesToAssemble, mainFile.getAbsolutePath());
            if (warnings != null && warnings.warningsOccurred()) {
                this.out.println(warnings.generateWarningReport());
            }
        } catch (final AssemblyException e) {
            Globals.exitCode = this.assembleErrorExitCode;
            this.out.println(e.errors().generateErrorAndWarningReport());
            this.out.println("Processing terminated due to errors.");
            return null;
        }
        // Setup for program simulation even if just assembling to prepare memory dumps
        program.setup(this.programArgumentList, null);
        if (this.simulate) {
            if (Globals.debug) {
                this.out.println("--------  SIMULATION BEGINS  -----------");
            }
            try {
                while (true) {
                    final Simulator.Reason done = program.simulate();
                    if (done == Simulator.Reason.MAX_STEPS) {
                        this.out.println("\nProgram terminated when maximum step limit " + this.options.maxSteps + " " +
                                "reached.");
                        break;
                    } else if (done == Simulator.Reason.CLIFF_TERMINATION) {
                        this.out.println("\nProgram terminated by dropping off the bottom.");
                        break;
                    } else if (done == Simulator.Reason.NORMAL_TERMINATION) {
                        this.out.println("\nProgram terminated by calling exit");
                        break;
                    }
                    assert done == Simulator.Reason.BREAKPOINT
                            : "Internal error: All cases other than breakpoints should be handled already";
                    this.displayAllPostMortem(program); // print registers if we hit a breakpoint, then continue
                }

            } catch (final SimulationException e) {
                Globals.exitCode = this.simulateErrorExitCode;
                this.out.println(e.errorMessage.generateReport());
                this.out.println("Simulation terminated due to errors.");
            }
            this.displayAllPostMortem(program);
        }
        if (Globals.debug) {
            this.out.println("\n--------  ALL PROCESSING COMPLETE  -----------");
        }
        return program;
    }

    /// ///////////////////////////////////////////////////////////////////
    // Displays any specified runtime properties. Initially just instruction count
    // DPS 19 July 2012
    private void displayMiscellaneousPostMortem(final Program program) {
        if (this.countInstructions) {
            this.out.println("\n" + Program.getRegisterValue("cycle"));
        }
    }

    /// ///////////////////////////////////////////////////////////////////
    // Displays requested register or registers
    private void displayRegistersPostMortem(final Program program) {
        // Display requested register contents
        for (final String reg : this.registerDisplayList) {
            if (FloatingPointRegisterFile.getRegister(reg) != null) {
                // TODO: do something for double vs float
                // It isn't clear to me what the best behaviour is
                // floating point register
                final int ivalue = Program.getRegisterValue(reg);
                final float fvalue = Float.intBitsToFloat(ivalue);
                if (this.verbose) {
                    this.out.print(reg + "\t");
                }
                if (this.displayFormat == Main.HEXADECIMAL) {
                    // display float (and double, if applicable) in hex
                    this.out.println(Binary.intToHexString(ivalue));

                } else if (this.displayFormat == Main.DECIMAL) {
                    // display float (and double, if applicable) in decimal
                    this.out.println(fvalue);

                } else { // displayFormat == ASCII
                    this.out.println(Binary.intToAscii(ivalue));
                }
            } else if (ControlAndStatusRegisterFile.getRegister(reg) != null) {
                this.out.print(reg + "\t");
                this.out.println(this.formatIntForDisplay((int) ControlAndStatusRegisterFile.getRegister(reg).getValue()));
            } else if (this.verbose) {
                this.out.print(reg + "\t");
                this.out.println(this.formatIntForDisplay((int) RegisterFile.getRegister(reg).getValue()));
            }
        }
    }

    /// ///////////////////////////////////////////////////////////////////
    // Formats int second for display: decimal, hex, ascii
    private String formatIntForDisplay(final int value) {
        return switch (this.displayFormat) {
            case Main.DECIMAL -> "" + value;
            case Main.ASCII -> Binary.intToAscii(value);
            default -> Binary.intToHexString(value); // hex case
        };
    }

    /// ///////////////////////////////////////////////////////////////////
    // Displays requested memory range or ranges
    private void displayMemoryPostMortem(final Memory memory) {
        int value;
        // Display requested memory range contents
        final Iterator<String> memIter = this.memoryDisplayList.iterator();
        int addressStart = 0, addressEnd = 0;
        while (memIter.hasNext()) {
            try { // This will succeed; error would have been caught during command arg parse
                addressStart = Binary.stringToInt(memIter.next());
                addressEnd = Binary.stringToInt(memIter.next());
            } catch (final NumberFormatException ignored) {
            }
            int valuesDisplayed = 0;
            for (int addr = addressStart; addr <= addressEnd; addr += Memory.WORD_LENGTH_BYTES) {
                if (addr < 0 && addressEnd > 0) {
                    break; // happens only if addressEnd is 0x7ffffffc
                }
                if (valuesDisplayed % Main.memoryWordsPerLine == 0) {
                    this.out.print((valuesDisplayed > 0) ? "\n" : "");
                    if (this.verbose) {
                        this.out.print("Mem[" + Binary.intToHexString(addr) + "]\t");
                    }
                }
                try {
                    // Allow display of binary text segment (machine code) DPS 14-July-2008
                    if (Memory.inTextSegment(addr)) {
                        final Integer iValue = memory.getRawWordOrNull(addr);
                        value = (iValue == null) ? 0 : iValue;
                    } else {
                        value = memory.getWord(addr);
                    }
                    this.out.print(this.formatIntForDisplay(value) + "\t");
                } catch (final AddressErrorException aee) {
                    this.out.print("Invalid address: " + addr + "\t");
                }
                valuesDisplayed++;
            }
            this.out.println();
        }
    }

    /// ////////////////////////////////////////////////////////////////////
    // If option to display RARS messages to standard err (System.err) is
    // present, it must be processed before all others. Since messages may
    // be output as early as during the command parse.
    private void processDisplayMessagesToErrSwitch(final String[] args, final String displayMessagesToErrSwitch) {
        for (final String arg : args) {
            if (arg.toLowerCase().equals(displayMessagesToErrSwitch)) {
                this.out = System.err;
                return;
            }
        }
    }

    /// ////////////////////////////////////////////////////////////////////
    // Decide whether copyright should be displayed, and display
    // if so.
    private void displayCopyright(final String[] args, final String noCopyrightSwitch) {
        for (final String arg : args) {
            if (arg.toLowerCase().equals(noCopyrightSwitch)) {
                return;
            }
        }
        this.out.println("RARS " + Globals.version + "  Copyright " + Globals.copyrightYears + " " + Globals.copyrightHolders
                + "\n");
    }

    /// ////////////////////////////////////////////////////////////////////
    // Display command line help text
    private void displayHelp() {
        final String[] segmentNames = MemoryDump.getSegmentNames();
        final StringBuilder segments = new StringBuilder();
        for (int i = 0; i < segmentNames.length; i++) {
            segments.append(segmentNames[i]);
            if (i < segmentNames.length - 1) {
                segments.append(", ");
            }
        }
        final List<DumpFormat> dumpFormats = DumpFormatLoader.getDumpFormats();
        final StringBuilder formats = new StringBuilder();
        for (int i = 0; i < dumpFormats.size(); i++) {
            formats.append(dumpFormats.get(i).getCommandDescriptor());
            if (i < dumpFormats.size() - 1) {
                formats.append(", ");
            }
        }
        final var helpMessage = """
                Usage:  Rars  [options] filename [additional filenames]
                
                Valid options (not case sensitive, separate by spaces) are:
                      a  -- assemble only, do not simulate
                  ae<n>  -- terminate RARS with integer exit code <n> if an assemble error occurs.
                """;
        this.out.println("  ascii  -- display memory or register contents interpreted as ASCII codes.");
        this.out.println("      b  -- brief - do not display register/memory address along with contents");
        this.out.println("      d  -- display RARS debugging statements");
        this.out.println("    dec  -- display memory or register contents in decimal.");
        this.out.println("   dump <segment> <format> <file> -- memory dump of specified memory segment");
        this.out.println("            in specified format to specified file.  Option may be repeated.");
        this.out.println("            Dump occurs at the end of simulation unless 'a' option is used.");
        this.out.println("            Segment and format are case-sensitive and possible values are:");
        this.out.println("            <segment> = " + segments + ", or a range like 0x400000-0x10000000              " +
                "       ");
        this.out.println("            <format> = " + formats);
        this.out.println("      g  -- force GUI mode");
        this.out.println("      h  -- display this help.  Use by itself with no filename.");
        this.out.println("    hex  -- display memory or register contents in hexadecimal (default)");
        this.out.println("     ic  -- display count of basic instructions 'executed'");
        this.out.println("     mc <config>  -- set memory configuration.  Argument <config> is");
        this.out.println("            case-sensitive and possible values are: Default for the default");
        this.out.println("            32-bit address space, CompactDataAtZero for a 32KB memory with");
        this.out.println("            data segment at address 0, or CompactTextAtZero for a 32KB");
        this.out.println("            memory with text segment at address 0.");
        this.out.println("     me  -- display RARS messages to standard err instead of standard out. ");
        this.out.println("            Can separate messages from program output using redirection");
        this.out.println("     nc  -- do not display copyright notice (for cleaner redirected/piped output).");
        this.out.println("     np  -- use of pseudo instructions and formats not permitted");
        this.out.println("      p  -- Project mode - assemble all files in the same directory as given file.");
        this.out.println("  se<n>  -- terminate RARS with integer exit code <n> if a simulation (run) error occurs.");
        this.out.println("     sm  -- start execution at statement with global label main, if defined");
        this.out.println("    smc  -- Self Modifying Code - Program can write and branch to either text or data segment");
        this.out.println("    rv64 -- Enables 64 bit assembly and executables (Not fully compatible with rv32)");
        this.out.println("    <n>  -- where <n> is an integer maximum count of steps to simulate.");
        this.out.println("            If 0, negative or not specified, there is no maximum.");
        this.out.println(" x<reg>  -- where <reg> is number or name (e.g. 5, t3, f10) of register whose ");
        this.out.println("            content to display at end of run.  Option may be repeated.");
        this.out.println("<reg_name>  -- where <reg_name> is name (e.g. t3, f10) of register whose");
        this.out.println("            content to display at end of run.  Option may be repeated. ");
        this.out.println("<m>-<n>  -- memory address range from <m> to <n> whose contents to");
        this.out.println("            display at end of run. <m> and <n> may be hex or decimal,");
        this.out.println("            must be on word boundary, <m> <= <n>.  Option may be repeated.");
        this.out.println("     pa  -- Program Arguments follow in a space-separated list.  This");
        this.out.println("            option must be placed AFTER ALL FILE NAMES, because everything");
        this.out.println("            that follows it is interpreted as a program argument to be");
        this.out.println("            made available to the program at runtime.");
        this.out.println("If more than one filename is listed, the first is assumed to be the main");
        this.out.println("unless the global statement label 'main' is defined in one of the files.");
        this.out.println("Exception handler not automatically assembled.  Add it to the file list.");
        this.out.println("Options used here do not affect RARS Settings menu values and vice versa.         ");
    }

}
