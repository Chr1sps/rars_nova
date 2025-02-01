package rars;

import com.formdev.flatlaf.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import rars.api.Program;
import rars.api.ProgramOptions;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryUtils;
import rars.settings.BoolSetting;
import rars.simulator.Simulator;
import rars.util.BinaryUtilsKt;
import rars.util.BinaryUtilsOld;
import rars.util.FilenameFinder;
import rars.util.RegisterUtils;
import rars.venus.VenusUI;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

import static rars.Globals.BOOL_SETTINGS;

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

    private final @NotNull ProgramOptions programOptions;
    private final @NotNull PrintStream out; // stream for display of command line output

    private Main(final @NotNull ProgramOptions programOptions) {
        this.programOptions = programOptions;
        this.out = programOptions.printToStdErr ? System.err : System.out;

        Globals.setupGlobalMemoryConfiguration(this.programOptions.memoryConfiguration);

        if (this.programOptions.gui) {
            Main.launchIDE(programOptions);
        } else {
            // running from command line.
            // assure command mode works in headless environment (generates exception if
            // not)
            System.setProperty("java.awt.headless", "true");

            this.runCommand();
            // this.dumpSegments(this.runCommand());
            System.exit(Globals.exitCode);
        }
    }

    public static void main(final String[] args) {
        final var programArgs = new ProgramOptions();
        new CommandLine(programArgs).execute(args);
        if (programArgs.showHelp) {
            CommandLine.usage(programArgs, System.out);
        } else {
            new Main(programArgs);
        }
    }

    /**
     * Check for memory address subrange. Has to be two integers separated
     * by "-"; no embedded spaces. e.g. 0x00400000-0x00400010
     * If number is not multiple of 4, will be rounded up to next higher.
     */
    private static String[] checkMemoryAddressRange(final @NotNull String arg) throws NumberFormatException {
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
            if (BinaryUtilsOld.stringToInt(memoryRange[0]) > BinaryUtilsOld.stringToInt(memoryRange[1]) ||
                !MemoryUtils.wordAligned(BinaryUtilsOld.stringToInt(memoryRange[0])) ||
                !MemoryUtils.wordAligned(BinaryUtilsOld.stringToInt(memoryRange[1]))) {
                throw new NumberFormatException();
            }
        }
        return memoryRange;
    }

    /**
     * There are no command arguments, so run in interactive mode by
     * launching the GUI-fronted integrated development environment.
     */
    private static void launchIDE(final @NotNull ProgramOptions options) {
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
        SwingUtilities.invokeLater(() -> Globals.GUI = new VenusUI("RARS " + Globals.version, options.files));
    }

    // /**
    //  * Perform any specified dump operations. See "dump" option.
    //  */
    // private void dumpSegments(final @Nullable Program program) {
    //     if (this.dumpTriples == null || program == null) {
    //         return;
    //     }
    //
    //     for (final String[] triple : this.dumpTriples) {
    //         final File file = new File(triple[2]);
    //         var segmentBounds = MemoryDump.getSegmentBounds(triple[0]);
    //         // If not segment name, see if it is address range instead. DPS 14-July-2008
    //         if (segmentBounds == null) {
    //             try {
    //                 final String[] memoryRange = Main.checkMemoryAddressRange(triple[0]);
    //                 segmentBounds = new Pair<>(
    //                     BinaryUtils.stringToInt(memoryRange[0]),
    //                     BinaryUtils.stringToInt(memoryRange[1])
    //                 );
    //             } catch (final NumberFormatException |
    //                 NullPointerException ignored) {
    //             }
    //         }
    //         if (segmentBounds == null) {
    //             this.out.println("Error while attempting to save dump, segment/address-range " + triple[0] + " is " +
    //                 "invalid!");
    //             continue;
    //         }
    //         final DumpFormat format = DumpFormats.findDumpFormatGivenCommandDescriptor(triple[1]);
    //         if (format == null) {
    //             this.out.println("Error while attempting to save dump, format " + triple[1] + " was not found!");
    //             continue;
    //         }
    //         try {
    //             final int highAddress = program.getMemory().getAddressOfFirstNull(
    //                 segmentBounds.first(),
    //                 segmentBounds.second()
    //             )
    //                 - DataTypes.WORD_SIZE;
    //             if (highAddress < segmentBounds.first()) {
    //                 this.out.println("This segment has not been written to, there is nothing to dump.");
    //                 continue;
    //             }
    //             format.dumpMemoryRange(file, segmentBounds.first(), highAddress, program.getMemory());
    //         } catch (final FileNotFoundException e) {
    //             this.out.println("Error while attempting to save dump, file " + file + " was not found!");
    //         } catch (final AddressErrorException e) {
    //             this.out.println("Error while attempting to save dump, file " + file + "!  Could not access address: "
    //                 + e.address + "!");
    //         } catch (final IOException e) {
    //             this.out.println("Error while attempting to save dump, file " + file + "!  Disk IO failed!");
    //         }
    //     }
    // }

    private void displayAllPostMortem(final @NotNull Program program) {
        this.displayMiscellaneousPostMortem();
        this.displayRegistersPostMortem();
        this.displayMemoryPostMortem(program.getMemory());
    }

    /** Carry out the RARS command: assemble then optionally run */
    private @Nullable Program runCommand() {
        if (this.programOptions.files.isEmpty()) {
            return null;
        }

        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, this.programOptions.isRV64);
        InstructionsRegistry.RV64_MODE_FLAG = this.programOptions.isRV64;

        final var mainFile = this.programOptions.files.getFirst().getAbsoluteFile();
        final @NotNull List<? extends @NotNull File> filesToAssemble;
        if (this.programOptions.isProjectMode) {
            final var allFoundProjectFiles = FilenameFinder.getFilenameListForDirectory(
                mainFile.getParentFile(),
                Globals.fileExtensions
            );
            // filesToAssemble = FilenameFinder.getFilenameList(mainFile.getParent(), Globals.fileExtensions);
            if (this.programOptions.files.size() > 1) {
                // Using "p" project option PLUS listing more than one file on command line.
                // Add the additional files, avoiding duplicates.
                final var nonMainFiles = this.programOptions.files.stream().skip(1).toList();
                final var moreFilesToAssemble = FilenameFinder.filterFilesByExtensions(
                    nonMainFiles,
                    Globals.fileExtensions
                );
                filesToAssemble = Stream.concat(
                    allFoundProjectFiles.stream(),
                    moreFilesToAssemble.stream()
                ).distinct().toList();
            } else {
                filesToAssemble = FilenameFinder.filterFilesByExtensions(
                    allFoundProjectFiles,
                    Globals.fileExtensions
                );
            }
        } else {
            // filtering this list when we don't want to assemble everything in a file is nonsense
            filesToAssemble = this.programOptions.files;
        }
        final Program program = new Program(this.programOptions);

        // program.assembleFiles(filesToAssemble, mainFile).fold(
        //     error -> {
        //        
        //     },
        //     errorList -> {
        //        
        //     }
        // );
        if (Globals.debug) {
            this.out.println("---  TOKENIZING & ASSEMBLY BEGINS  ---");
        }
        final var result = program.assembleFiles(filesToAssemble, mainFile);
        final var didSucceed = result.fold(
            assemblyError -> {
                Globals.exitCode = this.programOptions.assemblyErrorCode;
                this.out.println(assemblyError.errors.generateErrorAndWarningReport());
                this.out.println("Processing terminated due to errors.");
                return false;
            },
            right -> {
                if (right.warningsOccurred()) {
                    this.out.println(right.generateWarningReport());
                }
                return true;
            }
        );
        if (!didSucceed) {
            return null;
        }
        // Setup for program simulation even if just assembling to prepare memory dumps
        program.setup(this.programOptions.programArgs, null);
        if (!this.programOptions.assembleOnly) {
            if (Globals.debug) {
                this.out.println("--------  SIMULATION BEGINS  -----------");
            }
            try {
                while (true) {
                    final Simulator.Reason done = program.simulate();
                    if (done == Simulator.Reason.MAX_STEPS) {
                        this.out.println("\nProgram terminated when maximum step limit " + this.programOptions.maxSteps +
                            " " +
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
                Globals.exitCode = this.programOptions.simulationErrorCode;
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

    /**
     * Displays any specified runtime properties. Initially just instruction count
     * DPS 19 July 2012
     */
    private void displayMiscellaneousPostMortem() {
        if (this.programOptions.displayInstructionCount) {
            this.out.println("\n" + RegisterUtils.getRegisterValue("cycle"));
        }
    }

    /// Displays requested register or registers
    private void displayRegistersPostMortem() {
        // Display requested register contents
        for (final String registerName : this.programOptions.registers) {
            if (Globals.FP_REGISTER_FILE.getRegisterByName(registerName) != null) {
                // TODO: do something for double vs float
                // It isn't clear to me what the best behaviour is
                // floating point register
                final int ivalue = RegisterUtils.getRegisterValue(registerName);
                if (!this.programOptions.brief) {
                    this.out.print(registerName + "\t");
                }
                switch (this.programOptions.displayFormat) {
                    case HEX -> this.out.println(BinaryUtilsKt.intToHexStringWithPrefix(ivalue));
                    case DECIMAL -> {
                        final float fvalue = Float.intBitsToFloat(ivalue);
                        this.out.println(fvalue);
                    }
                    default -> this.out.println(BinaryUtilsOld.intToAscii(ivalue));
                }
            } else if (Globals.CS_REGISTER_FILE.getRegisterByName(registerName) != null) {
                this.out.print(registerName + "\t");
                this.out.println(this.formatIntForDisplay(Globals.CS_REGISTER_FILE.getLongValue(registerName)
                    .intValue()));
            } else if (this.programOptions.brief) {
                this.out.print(registerName + "\t");
                this.out.println(this.formatIntForDisplay((int) Globals.REGISTER_FILE.getRegisterByName(registerName)
                    .getValue()));
            }
        }
    }

    /// Formats int value for display: decimal, hex, ascii
    private @NotNull String formatIntForDisplay(final int value) {
        return switch (this.programOptions.displayFormat) {
            case DECIMAL -> Integer.toString(value);
            case HEX -> BinaryUtilsOld.intToAscii(value);
            default -> BinaryUtilsKt.intToHexStringWithPrefix(value); // hex case
        };
    }

    /// Displays requested memory range or ranges
    private void displayMemoryPostMortem(final Memory memory) {
        for (final var memoryRange : this.programOptions.memoryRanges) {

            final int startAddress = memoryRange.getFirst();
            final int endAddress = memoryRange.getSecond();
            int valuesDisplayed = 0;
            for (int addr = startAddress; addr <= endAddress; addr += DataTypes.WORD_SIZE) {
                if (addr < 0 && endAddress > 0) {
                    break; // happens only if addressEnd is 0x7ffffffc
                }
                if (valuesDisplayed % Main.memoryWordsPerLine == 0) {
                    this.out.print((valuesDisplayed > 0) ? "\n" : "");
                    if (!this.programOptions.brief) {
                        this.out.print("Mem[" + BinaryUtilsKt.intToHexStringWithPrefix(addr) + "]\t");
                    }
                }
                try {
                    // Allow display of binary text segment (machine code) DPS 14-July-2008
                    final int value;
                    if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(addr)) {
                        final var optValue = memory.getRawWordOrNull(addr);
                        value = (optValue == null) ? 0 : optValue;
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
}
