package rars.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.Globals;
import rars.ProgramStatement;
import rars.RISCVProgram;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.io.ConsoleIO;
import rars.riscv.hardware.InterruptController;
import rars.riscv.hardware.Memory;
import rars.settings.BoolSetting;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import static rars.Globals.BOOL_SETTINGS;

/**
 * <p>
 * This is most of the public API for running RARS programs. It wraps internal
 * APIs to provide a base for making applications to simulate many programs.
 * </p>
 * <p>
 * The order you are expected to run the methods is:
 * <ol>
 * <li>assemble(...)
 * <li>setup(...)
 * <li>get/set for any specific setup
 * <li>simulate()
 * <li>get/set to check output
 * <li>repeat 3-5 if simulation hasn't terminated
 * <li>repeat 2-6 for multiple inputs as needed
 * </ol>
 *
 * <p>
 * Importantly, only one instance of Program can be setup at a time (this may
 * change in the future). Only the most recent program to be setup is valid to
 * call simulate on. Additionally, reading registers or memory is also only
 * valid
 * once setup has been called and before another setup is called.
 * </p>
 *
 * <p>
 * Also, it is not threadsafe, calling assemble in another thread could
 * invalidate
 * a concurrent simulation.
 * </p>
 */
public final class Program {

    private final @NotNull RISCVProgram code;
    private final @NotNull Memory assembled;
    private final @NotNull Memory simulation;
    private final @NotNull ProgramOptions programOptions;
    private @NotNull ByteArrayOutputStream stdout, stderr;
    private int startPC, exitCode;
    private ConsoleIO consoleIO;

    public Program(final @NotNull ProgramOptions programOptions) {
        this.programOptions = programOptions;
        this.code = new RISCVProgram();
        this.assembled = new Memory(this.programOptions.memoryConfiguration);
        this.simulation = new Memory(this.programOptions.memoryConfiguration);
    }

    /**
     * Assembles from a list of files
     *
     * @param files
     *     A list of files to assemble
     * @param mainFile
     *     Which file should be considered the main file; it should be in
     *     files
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException
     *     thrown if any errors are found in the code
     */
    public @NotNull ErrorList assembleFiles(
        final @NotNull List<@NotNull File> files,
        final @NotNull File mainFile
    ) throws AssemblyException {
        final var programs = this.code.prepareFilesForAssembly(files, mainFile, null);
        return this.assemble(programs);
    }

    /**
     * Assembles a single file
     *
     * @param file
     *     path to the file to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException
     *     thrown if any errors are found in the code
     */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ErrorList assembleFile(final @NotNull File file) throws AssemblyException {
        final var programs = this.code.prepareFilesForAssembly(List.of(file), file, null);
        return this.assemble(programs);
    }

    /**
     * Assembles a string as RISC-V source code
     *
     * @param source
     *     the code to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException
     *     thrown if any errors are found in the code
     */
    public @NotNull ErrorList assembleString(final @NotNull String source) throws AssemblyException {
        this.code.fromString(source);
        this.code.tokenize();
        final var programs = List.of(this.code);
        return this.assemble(programs);
    }

    private @NotNull ErrorList assemble(final @NotNull List<@NotNull RISCVProgram> programs) throws AssemblyException {
        Globals.REGISTER_FILE.setValuesFromConfiguration(this.assembled.getMemoryConfiguration());
        // Assembling changes memory so we need to swap to capture that.
        final Memory temp = Globals.swapMemoryInstance(this.assembled);
        try {
            final var errorList = this.code.assemble(
                programs,
                this.programOptions.usePseudoInstructions,
                this.programOptions.warningsAreErrors
            );
            Globals.swapMemoryInstance(temp);

            Globals.REGISTER_FILE.initializeProgramCounter(this.programOptions.startAtMain);
            this.startPC = Globals.REGISTER_FILE.getProgramCounter();

            return errorList;
        } catch (final AssemblyException ae) {
            Globals.swapMemoryInstance(temp);
            throw ae;
        }
    }

    /**
     * Prepares the simulator for execution. Clears registers, loads arguments
     * into memory and initializes the String backed STDIO
     *
     * @param args
     *     Just like the args to a Java main, but an ArrayList.
     * @param STDIN
     *     A string that can be read in the program like its stdin or null
     *     to allow IO passthrough
     */
    public void setup(final @NotNull List<@NotNull String> args, final @Nullable String STDIN) {
        final var tmpMem = Globals.swapMemoryInstance(this.simulation);
        new ProgramArgumentList(args).storeProgramArguments();
        Globals.swapMemoryInstance(tmpMem);

        Globals.REGISTER_FILE.resetRegisters();
        Globals.FP_REGISTER_FILE.resetRegisters();
        Globals.CS_REGISTER_FILE.resetRegisters();
        InterruptController.reset();
        Globals.REGISTER_FILE.initializeProgramCounter(this.startPC);
        Globals.exitCode = 0;

        // Copy in assembled code and arguments
        this.simulation.copyFrom(this.assembled);

        // To capture the IO we need to replace stdin and friends
        if (STDIN != null) {
            this.stdout = new ByteArrayOutputStream();
            this.stderr = new ByteArrayOutputStream();
            this.consoleIO = new ConsoleIO(
                new ByteArrayInputStream(STDIN.getBytes()),
                this.stdout,
                this.stderr,
                BOOL_SETTINGS
            );
        } else {
            this.consoleIO = new ConsoleIO(
                System.in,
                System.out,
                System.err,
                BOOL_SETTINGS
            );
        }
    }

    /**
     * Simulates a processor executing the machine code.
     *
     * @return the reason why simulation was paused or terminated.
     * Possible values are:
     * <ul>
     * <li>BREAKPOINT (caused by ebreak instruction),
     * <li>MAX_STEPS (caused by simulating Options.maxSteps instructions),
     * <li>NORMAL_TERMINATION (caused by executing the exit system call)
     * <li>CLIFF_TERMINATION (caused by the program overflowing the written
     * code).
     * </ul>
     * Only BREAKPOINT and MAX_STEPS can be simulated further.
     * @throws SimulationException
     *     thrown if there is an uncaught interrupt. The
     *     program cannot be simulated further.
     */
    public @NotNull Simulator.Reason simulate() throws SimulationException {

        // Swap out global state for local state.
        final boolean selfMod = BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED);
        BOOL_SETTINGS.setSetting(
            BoolSetting.SELF_MODIFYING_CODE_ENABLED,
            this.programOptions.selfModifyingCode
        );
        final Memory tmpMem = Globals.swapMemoryInstance(this.simulation);

        SimulationException e = null;
        Simulator.Reason ret = null;
        try {
            ret = Globals.SIMULATOR.simulateCli(
                Globals.REGISTER_FILE.getProgramCounter(),
                this.programOptions.maxSteps,
                this.consoleIO
            );
        } catch (final SimulationException se) {
            e = se;
        }
        this.exitCode = Globals.exitCode;

        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, selfMod);
        Globals.swapMemoryInstance(tmpMem);

        if (e != null) {
            throw e;
        }
        return ret;
    }

    /**
     * <p>getSTDOUT.</p>
     *
     * @return converts the bytes sent to stdout into a string (resets to "" when
     * setup is called)
     */
    public @NotNull String getSTDOUT() {
        return this.stdout.toString();
    }

    /**
     * <p>getSTDERR.</p>
     *
     * @return converts the bytes sent to stderr into a string (resets to "" when
     * setup is called)
     */
    public @NotNull String getSTDERR() {
        return this.stderr.toString();
    }

    /**
     * Returns the exit code passed to the exit syscall if it was called, otherwise
     * returns 0
     *
     * @return a int
     */
    public int getExitCode() {
        return this.exitCode;
    }

    /**
     * Gets the instance of memory the program is using.
     * <p>
     * This is only valid when setup has been called.
     *
     * @return a {@link Memory} object
     */
    public Memory getMemory() {
        return this.simulation;
    }

    public List<ProgramStatement> getParsedList() {
        return this.code.getParsedList();
    }

    public List<ProgramStatement> getMachineList() {
        return this.code.getMachineList();
    }
}
