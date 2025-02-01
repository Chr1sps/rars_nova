package rars.api

import arrow.core.Either
import arrow.core.raise.either
import rars.ErrorList
import rars.Globals
import rars.ProgramStatement
import rars.RISCVProgram
import rars.exceptions.AssemblyError
import rars.exceptions.SimulationEvent
import rars.io.ConsoleIO
import rars.riscv.hardware.Memory
import rars.settings.BoolSetting
import rars.simulator.ProgramArgumentList
import rars.simulator.Simulator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 *
 *
 * This is most of the public API for running RARS programs. It wraps internal
 * APIs to provide a base for making applications to simulate many programs.
 *
 *
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
 *
 *
 *
 * Importantly, only one instance of Program can be setup at a time (this may
 * change in the future). Only the most recent program to be setup is valid to
 * call simulate on. Additionally, reading registers or memory is also only
 * valid
 * once setup has been called and before another setup is called.
 *
 *
 *
 *
 * Also, it is not thread safe, calling assemble in another thread could
 * invalidate
 * a concurrent simulation.
 *
 */
class Program(private val programOptions: ProgramOptions) {
    private val code: RISCVProgram = RISCVProgram()
    private val assembled: Memory = Memory(this.programOptions.memoryConfiguration)

    /**
     * Gets the instance of memory the program is using.
     *
     *
     * This is only valid when setup has been called.
     *
     * @return a [Memory] object
     */
    val memory: Memory = Memory(this.programOptions.memoryConfiguration)
    private var stdout: ByteArrayOutputStream? = null
    private var stderr: ByteArrayOutputStream? = null
    private var startPC = 0

    /**
     * Returns the exit code passed to the exit syscall if it was called, otherwise
     * returns 0
     *
     * @return a int
     */
    var exitCode: Int = 0
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
        files: MutableList<out File>,
        mainFile: File
    ): Either<AssemblyError, ErrorList> = either {
        val programs = this@Program.code.prepareFilesForAssembly(files, mainFile, null).bind()
        this@Program.assemble(programs).bind()
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
        val programs = this@Program.code.prepareFilesForAssembly(listOf(file), file, null).bind()
        this@Program.assemble(programs).bind()
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
        this@Program.code.fromString(source)
        this@Program.code.tokenize().bind()
        this@Program.assemble(listOf(this@Program.code)).bind()
    }

    private fun assemble(programs: List<RISCVProgram>): Either<AssemblyError, ErrorList> = either {
        Globals.REGISTER_FILE.setValuesFromConfiguration(this@Program.assembled.memoryConfiguration)
        // Assembling changes memory so we need to swap to capture that.
        val temp = Globals.swapMemoryInstance(this@Program.assembled)
        val errorList = this@Program.code.assemble(
            programs,
            this@Program.programOptions.usePseudoInstructions,
            this@Program.programOptions.warningsAreErrors
        ).onLeft {
            Globals.swapMemoryInstance(temp)
            raise(it)
        }.bind()
        Globals.swapMemoryInstance(temp)

        Globals.REGISTER_FILE.initializeProgramCounter(this@Program.programOptions.startAtMain)
        this@Program.startPC = Globals.REGISTER_FILE.programCounter
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
    fun setup(args: MutableList<String>, stdin: String?) {
        val tmpMem = Globals.swapMemoryInstance(this.memory)
        ProgramArgumentList(args).storeProgramArguments()
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
            this.stdout = ByteArrayOutputStream()
            this.stderr = ByteArrayOutputStream()
            this.consoleIO = ConsoleIO(
                ByteArrayInputStream(stdin.toByteArray()),
                this.stdout!!,
                this.stderr!!,
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
    fun simulate(): Either<SimulationEvent, Simulator.Reason> {
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

    val sTDOUT: String
        /**
         *
         * getSTDOUT.
         *
         * @return converts the bytes sent to stdout into a string (resets to "" when
         * setup is called)
         */
        get() = this.stdout.toString()

    val sTDERR: String
        /**
         *
         * getSTDERR.
         *
         * @return converts the bytes sent to stderr into a string (resets to "" when
         * setup is called)
         */
        get() = this.stderr.toString()

    val parsedList: List<ProgramStatement>?
        get() = this.code.parsedList

    val machineList: List<ProgramStatement>
        get() = this.code.getMachineList()
}
