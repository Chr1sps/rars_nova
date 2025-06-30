package rars

import com.formdev.flatlaf.util.SystemInfo
import picocli.CommandLine
import rars.api.DisplayFormat
import rars.api.Program
import rars.api.ProgramOptions
import rars.assembler.DataTypes
import rars.logging.RARSLogging
import rars.logging.fatal
import rars.riscv.InstructionsRegistry
import rars.riscv.hardware.memory.Memory
import rars.riscv.hardware.memory.wordAligned
import rars.settings.BoolSetting
import rars.simulator.StoppingEvent
import rars.util.toAscii
import rars.util.toHexStringWithPrefix
import rars.util.translateToInt
import rars.venus.VenusUI
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.io.File
import java.io.PrintStream
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * Launch the application
 *
 * @author Pete Sanderson
 * @version December 2009
 */
class Main internal constructor(private val programOptions: ProgramOptions) {
    private val out: PrintStream =
        if (programOptions.printToStdErr) System.err else System.out

    init {
        Globals.setupGlobalMemoryConfiguration(this.programOptions.memoryConfiguration)

        if (this.programOptions.gui) {
            launchIDE(programOptions)
        } else {
            // running from command line.
            // assure command mode works in headless environment (generates exception if
            // not)
            System.setProperty("java.awt.headless", "true")

            this.runCommand()
            // this.dumpSegments(this.runCommand());
            exitProcess(Globals.exitCode)
        }
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
    //         // If not segment name, see if it is address range instead.
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
    private fun displayAllPostMortem(program: Program) {
        this.displayMiscellaneousPostMortem()
        this.displayRegistersPostMortem()
        this.displayMemoryPostMortem(program.memory)
    }

    /** Carry out the RARS command: assemble then optionally run  */
    private fun runCommand(): Program? {
        if (this.programOptions.files.isEmpty()) {
            return null
        }

        Globals.BOOL_SETTINGS.setSetting(
            BoolSetting.RV64_ENABLED,
            this.programOptions.isRV64
        )
        InstructionsRegistry.RV64_MODE_FLAG = this.programOptions.isRV64

        val mainFile: File = this.programOptions.files.first().absoluteFile
        val filesToAssemble = if (this.programOptions.isProjectMode) {
            val allFoundProjectFiles = mainFile.parentFile.listFiles { file ->
                file.extension in Globals.FILE_EXTENSIONS
            }
            if (this.programOptions.files.size > 1) {
                // Using "p" project option PLUS listing more than one file on command line.
                // Add the additional files, avoiding duplicates.
                val nonMainFiles = this.programOptions.files.drop(1)
                val moreFilesToAssemble = nonMainFiles.filter { file ->
                    file.extension in Globals.FILE_EXTENSIONS
                }
                (allFoundProjectFiles + moreFilesToAssemble).distinct()
            } else allFoundProjectFiles.filter { file ->
                file.extension in Globals.FILE_EXTENSIONS
            }

        } else {
            // filtering this list when we don't want to assemble everything in a file is nonsense
            this.programOptions.files
        }
        val program = Program(this.programOptions)

        if (Globals.debug) {
            this.out.println("---  TOKENIZING & ASSEMBLY BEGINS  ---")
        }
        val result = program.assembleFiles(filesToAssemble, mainFile)
        val didSucceed = result.fold<Boolean>(
            { assemblyError ->
                Globals.exitCode = this.programOptions.assemblyErrorCode
                this.out.println(assemblyError.errors.generateErrorAndWarningReport())
                this.out.println("Processing terminated due to errors.")
                false
            },
            { right ->
                if (right.warningsOccurred()) {
                    this.out.println(right.generateWarningReport())
                }
                true
            }
        )
        if (!didSucceed) {
            return null
        }
        // Setup for program simulation even if just assembling to prepare memory dumps
        program.setup(this.programOptions.programArgs, null)
        if (!this.programOptions.assembleOnly) {
            if (Globals.debug) {
                this.out.println("--------  SIMULATION BEGINS  -----------")
            }
            while (true) {
                val doLoop = program.simulate().let { stoppingEvent ->
                    when (stoppingEvent) {
                        is StoppingEvent.ErrorHit -> {
                            Globals.exitCode =
                                programOptions.simulationErrorCode
                            out.println(stoppingEvent.error.message.generateReport())
                            out.println("Simulation terminated due to errors.")
                            false
                        }
                        StoppingEvent.MaxStepsHit -> {
                            out.println(
                                """
                                Program terminated when maximum step limit ${programOptions.maxSteps} reached.
                                """.trimIndent()
                            )
                            false
                        }
                        StoppingEvent.CliffTermination -> {
                            this.out.println("\nProgram terminated by dropping off the bottom.")
                            false
                        }
                        StoppingEvent.NormalTermination -> {
                            this.out.println("\nProgram terminated by calling exit")
                            false
                        }
                        StoppingEvent.BreakpointHit -> {
                            displayAllPostMortem(program) // print registers if we hit a breakpoint, then continue
                            true
                        }
                        else -> error("Invalid stopping event: $stoppingEvent")
                    }
                }
                if (!doLoop) break
            }
            this.displayAllPostMortem(program)
        }
        if (Globals.debug) {
            this.out.println("\n--------  ALL PROCESSING COMPLETE  -----------")
        }
        return program
    }

    /**
     * Displays any specified runtime properties. Initially just instruction count
     */
    private fun displayMiscellaneousPostMortem() {
        if (this.programOptions.displayInstructionCount) {
            this.out.println("\n" + getRegisterValue("cycle"))
        }
    }

    /** Displays requested register or registers */
    private fun displayRegistersPostMortem() {
        // Display requested register contents
        for (registerName in this.programOptions.registers) {
            when {
                Globals.FP_REGISTER_FILE.getRegisterByName(registerName) != null -> {
                    // TODO: do something for double vs float
                    // It isn't clear to me what the best behaviour is
                    // floating point register
                    val ivalue = getRegisterValue(registerName)!!
                    if (!this.programOptions.brief) {
                        this.out.print(registerName + "\t")
                    }
                    when (this.programOptions.displayFormat) {
                        DisplayFormat.HEX -> this.out.println(ivalue.toHexStringWithPrefix())
                        DisplayFormat.DECIMAL -> {
                            val fvalue = Float.fromBits(ivalue)
                            this.out.println(fvalue)
                        }

                        else -> this.out.println(ivalue.toAscii())
                    }
                }
                Globals.CS_REGISTER_FILE.getRegisterByName(registerName) != null -> {
                    this.out.print(registerName + "\t")
                    this.out.println(
                        this.formatIntForDisplay(
                            Globals.CS_REGISTER_FILE.getLong(registerName)!!
                                .toInt()
                        )
                    )
                }
                this.programOptions.brief -> {
                    this.out.print(registerName + "\t")
                    this.out.println(
                        this.formatIntForDisplay(
                            Globals.REGISTER_FILE.getRegisterByName(registerName)!!
                                .getValue().toInt()
                        )
                    )
                }
            }
        }
    }

    /** Formats int value for display: decimal, hex, ascii */
    private fun formatIntForDisplay(value: Int): String {
        return when (this.programOptions.displayFormat) {
            DisplayFormat.DECIMAL -> value.toString()
            DisplayFormat.HEX -> value.toAscii()
            else -> value.toHexStringWithPrefix()
        }
    }

    /** Displays requested memory range or ranges */
    private fun displayMemoryPostMortem(memory: Memory) {
        for (memoryRange in this.programOptions.memoryRanges) {
            val startAddress = memoryRange.first
            val endAddress = memoryRange.second
            var valuesDisplayed = 0
            var addr = startAddress
            while (addr <= endAddress) {
                if (addr < 0 && endAddress > 0) {
                    break // happens only if addressEnd is 0x7ffffffc
                }
                if (valuesDisplayed % MEMORY_WORDS_PER_LINE == 0) {
                    this.out.print(if (valuesDisplayed > 0) "\n" else "")
                    if (!this.programOptions.brief) {
                        this.out.print("Mem[" + addr.toHexStringWithPrefix() + "]\t")
                    }
                }
                val eitherAddress =
                    if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(addr)) {
                        memory.getRawWordOrNull(addr).map { it ?: 0 }
                    } else {
                        memory.getWord(addr)
                    }
                eitherAddress.fold(
                    { error -> out.print("Invalid address: $addr\t") },
                    { value -> out.print(formatIntForDisplay(value) + "\t") }
                )
                valuesDisplayed++
                addr += DataTypes.WORD_SIZE
            }
            this.out.println()
        }
    }

    // TODO: remove in favour of something that doesn't use Globals
    /**
     * Gets the value of a normal, floating-point or control and status register.
     *
     * @param name
     * Either the common usage (t0, a0, ft0), explicit numbering (x2,
     * x3, f0), or CSR name (ustatus)
     * @return The value of the register as an int (floats are encoded as IEEE-754)
     * @throws NullPointerException
     * if name is invalid; only needs to be checked if
     * code accesses arbitrary names
     */
    private fun getRegisterValue(name: String): Int? =
        Globals.REGISTER_FILE.getInt(name)
            ?: Globals.FP_REGISTER_FILE.getInt(name)
            ?: Globals.CS_REGISTER_FILE.getInt(name)
}

fun main(args: Array<String>) {
    val programArgs = ProgramOptions()
    CommandLine(programArgs).execute(*args)
    if (programArgs.showHelp) {
        CommandLine.usage(programArgs, System.out)
    } else {
        Main(programArgs)
    }
}

private const val RANGE_SEPARATOR = "-"
private const val MEMORY_WORDS_PER_LINE =
    4 // display 4 memory words, tab separated, per line

/**
 * Check for memory address subrange. Has to be two integers separated
 * by "-"; no embedded spaces. e.g. 0x00400000-0x00400010
 * If number is not multiple of 4, will be rounded up to next higher.
 */
@Throws(NumberFormatException::class)
private fun checkMemoryAddressRange(arg: String): Array<String>? = if (
    arg.contains(RANGE_SEPARATOR)
) {
    // assume correct format, two numbers separated by -, no embedded spaces.
    // If that doesn't work it is invalid.
    val index = arg.indexOf(RANGE_SEPARATOR)
    val memoryRange = arrayOf(
        arg.substring(0, index),
        arg.substring(index + 1)
    )
    // NOTE: I will use homegrown decoder, because Integer.decode will throw
    // exception on address higher than 0x7FFFFFFF (e.g. sign bit is 1).
    val first = memoryRange[0].translateToInt()!!
    val second = memoryRange[1].translateToInt()!!
    if (first > second ||
        !wordAligned(first) ||
        !wordAligned(second)
    ) throw NumberFormatException()
    memoryRange
} else null

/**
 * There are no command arguments, so run in interactive mode by
 * launching the GUI-fronted integrated development environment.
 */
private fun launchIDE(options: ProgramOptions) {
    System.setProperty("apple.laf.useScreenMenuBar", "true") // Puts RARS menu
    // on Mac OS menu bar
    if (SystemInfo.isLinux) {
        JFrame.setDefaultLookAndFeelDecorated(true)
        JDialog.setDefaultLookAndFeelDecorated(true)
    }
    if (SystemInfo.isMacOS) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "RARS Nova")
        System.setProperty("apple.awt.application.appearance", "system")
    }
    Toolkit.getDefaultToolkit()
        .systemEventQueue
        .push(MyEventQueue())
    Thread.setDefaultUncaughtExceptionHandler { thread, e ->
        RootLogger.fatal(exception = e) {
            """Uncaught exception in thread "${thread.name}" """
        }
    }
    SwingUtilities.invokeLater {
        Globals.GUI = VenusUI("RARS " + Globals.VERSION, options.files)
    }
}

val RootLogger = RARSLogging.forName("RARSRootLogger")

private class MyEventQueue : EventQueue() {
    override fun dispatchEvent(event: AWTEvent?) {
        try {
            super.dispatchEvent(event)
        } catch (e: Exception) {
            RootLogger.fatal(exception = e) { "Exception in Swing EventQueue while dispatching event $event" }
        }
    }
}