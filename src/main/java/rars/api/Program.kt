package rars.api

import arrow.core.Either
import arrow.core.raise.either
import rars.ErrorList
import rars.Globals
import rars.RISCVProgram
import rars.exceptions.AssemblyError
import rars.exceptions.SimulationError
import rars.io.ConsoleIO
import rars.riscv.hardware.Memory
import rars.settings.BoolSetting
import rars.simulator.Simulator
import rars.simulator.storeProgramArguments
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * This is most of the public API for running RARS programs. It wraps internal
 * APIs to provide a base for making applications to simulate many programs.
 *
 * The order you are expected to run the methods is:
 *
 *  1. assemble(...)
 *  2. setup(...)
 *  3. get/set for any specific setup
 *  4. simulate()
 *  5. get/set to check output
 *  6. repeat 3-5 if simulation hasn't terminated
 *  7. repeat 2-6 for multiple inputs as needed
 *
 * Importantly, only one instance of Program can be setup at a time (this may
 * change in the future). Only the most recent program to be setup is valid to
 * call simulate on. Additionally, reading registers or memory is also only
 * valid
 * once setup has been called and before another setup is called.
 *
 * Also, it is not thread safe, calling assemble in another thread could
 * invalidate
 * a concurrent simulation.
 */
class Program(private val programOptions: ProgramOptions) {
    private val code: RISCVProgram = RISCVProgram()
    private val assembled: Memory = Memory(this.programOptions.memoryConfiguration)

    /**
     * The instance of memory the program is using.
     * This is only valid when setup has been called.
     */
    val memory = Memory(this.programOptions.memoryConfiguration)
    private var outStream: ByteArrayOutputStream? = null
    private var errStream: ByteArrayOutputStream? = null
    private var startPC = 0

    /** The exit code of the program. */
    var exitCode = 0
        private set
    private var consoleIO: ConsoleIO? = null

    /**
     * Assembles from a list of files
     *
     * @param files
     * A list of files to assemble
     * @param mainFile
     * Which file should be considered the main file; it should be in
     * files
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyError
     * thrown if any errors are found in the code
     */
    fun assembleFiles(
        files: List<File>,
        mainFile: File
    ): Either<AssemblyError, ErrorList> = either {
        val programs = code.prepareFilesForAssembly(files, mainFile, null).bind()
        assemble(programs).bind()
    }

    /**
     * Assembles a single file
     *
     * @param file
     * path to the file to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyError
     * thrown if any errors are found in the code
     */
    fun assembleFile(file: File): Either<AssemblyError, ErrorList> = either {
        val programs = code.prepareFilesForAssembly(listOf(file), file, null).bind()
        assemble(programs).bind()
    }

    /**
     * Assembles a string as RISC-V source code
     *
     * @param source
     * the code to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyError
     * thrown if any errors are found in the code
     */
    fun assembleString(source: String): Either<AssemblyError, ErrorList> = either {
        code.fromString(source)
        code.tokenize().bind()
        assemble(listOf(code)).bind()
    }

    private fun assemble(programs: List<RISCVProgram>): Either<AssemblyError, ErrorList> = either {
        Globals.REGISTER_FILE.setValuesFromConfiguration(assembled.memoryConfiguration)
        // Assembling changes memory so we need to swap to capture that.
        val temp = Globals.swapMemoryInstance(assembled)
        val errorList = code.assemble(
            programs,
            programOptions.usePseudoInstructions,
            programOptions.warningsAreErrors
        ).onLeft {
            Globals.swapMemoryInstance(temp)
            raise(it)
        }.bind()
        Globals.swapMemoryInstance(temp)

        Globals.REGISTER_FILE.initializeProgramCounter(programOptions.startAtMain)
        startPC = Globals.REGISTER_FILE.programCounter
        errorList

    }

    /**
     * Prepares the simulator for execution. Clears registers, loads arguments
     * into memory and initializes the String backed STDIO
     *
     * @param args
     * Just like the args to a Java main, but an ArrayList.
     * @param stdin
     * A string that can be read in the program like its stdin or null
     * to allow IO passthrough
     */
    fun setup(args: List<String>, stdin: String?) {
        val tmpMem = Globals.swapMemoryInstance(this.memory)
        storeProgramArguments(args)
        Globals.swapMemoryInstance(tmpMem)

        Globals.REGISTER_FILE.resetRegisters()
        Globals.FP_REGISTER_FILE.resetRegisters()
        Globals.CS_REGISTER_FILE.resetRegisters()
        Globals.INTERRUPT_CONTROLLER.reset()
        Globals.REGISTER_FILE.initializeProgramCounter(this.startPC)
        Globals.exitCode = 0

        // Copy in assembled code and arguments
        this.memory.copyFrom(this.assembled)

        // To capture the IO we need to replace stdin and friends
        if (stdin != null) {
            this.outStream = ByteArrayOutputStream()
            this.errStream = ByteArrayOutputStream()
            this.consoleIO = ConsoleIO(
                ByteArrayInputStream(stdin.toByteArray()),
                this.outStream!!,
                this.errStream!!,
                Globals.BOOL_SETTINGS
            )
        } else {
            this.consoleIO = ConsoleIO(
                System.`in`,
                System.out,
                System.err,
                Globals.BOOL_SETTINGS
            )
        }
    }

    /**
     * Simulates a processor executing the machine code.
     *
     * @return the reason why simulation was paused or terminated.
     * Possible values are:
     *
     *  * BREAKPOINT (caused by ebreak instruction),
     *  * MAX_STEPS (caused by simulating Options.maxSteps instructions),
     *  * NORMAL_TERMINATION (caused by executing the exit system call)
     *  * CLIFF_TERMINATION (caused by the program overflowing the written
     * code).
     *
     * Only BREAKPOINT and MAX_STEPS can be simulated further.
     * @throws SimulationError
     * thrown if there is an uncaught interrupt. The
     * program cannot be simulated further.
     */
    fun simulate(): Either<SimulationError, Simulator.Reason> {
        // Swap out global state for local state.

        val selfMod = Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)
        Globals.BOOL_SETTINGS.setSetting(
            BoolSetting.SELF_MODIFYING_CODE_ENABLED,
            this.programOptions.selfModifyingCode
        )
        val tmpMem = Globals.swapMemoryInstance(this.memory)

        val result = Globals.SIMULATOR.simulateCli(
            Globals.REGISTER_FILE.programCounter,
            this.programOptions.maxSteps,
            this.consoleIO!!
        )
        this.exitCode = Globals.exitCode

        Globals.BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, selfMod)
        Globals.swapMemoryInstance(tmpMem)

        return result
    }

    /** Bytes sent to stdout as a string. */
    val stdout get() = this.outStream.toString()

    /** Bytes sent to stderr as a string. */
    val stderr get() = this.errStream.toString()

    val parsedList get() = this.code.parsedList
    val machineList get() = this.code.getMachineList()
}
