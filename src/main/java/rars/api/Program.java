package rars.api;

import org.jetbrains.annotations.NotNull;
import rars.ErrorList;
import rars.Globals;
import rars.ProgramStatement;
import rars.RISCVProgram;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.hardware.*;
import rars.settings.BoolSetting;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;
import rars.util.SystemIO;

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
    private SystemIO.Data fds;
    private @NotNull ByteArrayOutputStream stdout, stderr;
    private int startPC, exitCode;

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
    public ErrorList assembleFiles(
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
    public ErrorList assembleFile(final @NotNull File file) throws AssemblyException {
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
    public ErrorList assembleString(final @NotNull String source) throws AssemblyException {
        this.code.fromString(source);
        this.code.tokenize();
        final var programs = List.of(this.code);
        return this.assemble(programs);
    }

    private ErrorList assemble(final @NotNull List<RISCVProgram> programs) throws AssemblyException {
        Globals.REGISTER_FILE.setValuesFromConfiguration(this.assembled.getMemoryConfiguration());
        final Memory temp = Globals.swapInstance(this.assembled); // Assembling changes memory so we need to swap to 
        // capture that.
        ErrorList warnings = null;
        AssemblyException e = null;
        try {
            warnings = this.code.assemble(
                programs,
                this.programOptions.usePseudoInstructions,
                this.programOptions.warningsAreErrors
            );
        } catch (final AssemblyException ae) {
            e = ae;
        }
        Globals.swapInstance(temp);
        if (e != null) {
            throw e;
        }

        Globals.REGISTER_FILE.initializeProgramCounter(this.programOptions.startAtMain);
        this.startPC = Globals.REGISTER_FILE.getProgramCounter();

        return warnings;
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
    public void setup(final @NotNull List<String> args, final String STDIN) {
        final var tmpMem = Globals.swapInstance(this.simulation);
        new ProgramArgumentList(args).storeProgramArguments();
        Globals.swapInstance(tmpMem);

        Globals.REGISTER_FILE.resetRegisters();
        FloatingPointRegisterFile.resetRegisters();
        ControlAndStatusRegisterFile.resetRegisters();
        InterruptController.reset();
        Globals.REGISTER_FILE.initializeProgramCounter(this.startPC);
        Globals.exitCode = 0;

        // Copy in assembled code and arguments
        this.simulation.copyFrom(this.assembled);

        // To capture the IO we need to replace stdin and friends
        if (STDIN != null) {
            this.stdout = new ByteArrayOutputStream();
            this.stderr = new ByteArrayOutputStream();
            this.fds = new SystemIO.Data(
                new ByteArrayInputStream(STDIN.getBytes()), this.stdout, this.stderr);
        } else {
            this.fds = new SystemIO.Data(true);
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
    public Simulator.Reason simulate() throws SimulationException {

        // Swap out global state for local state.
        final boolean selfMod = BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED);
        BOOL_SETTINGS.setSetting(
            BoolSetting.SELF_MODIFYING_CODE_ENABLED,
            this.programOptions.selfModifyingCode
        );
        final SystemIO.Data tmpFiles = SystemIO.swapData(this.fds);
        final Memory tmpMem = Globals.swapInstance(this.simulation);

        SimulationException e = null;
        Simulator.Reason ret = null;
        try {
            ret = RISCVProgram.simulate(this.programOptions.maxSteps);
        } catch (final SimulationException se) {
            e = se;
        }
        this.exitCode = Globals.exitCode;

        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, selfMod);
        SystemIO.swapData(tmpFiles);
        Globals.swapInstance(tmpMem);

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
    public String getSTDOUT() {
        return this.stdout.toString();
    }

    /**
     * <p>getSTDERR.</p>
     *
     * @return converts the bytes sent to stderr into a string (resets to "" when
     * setup is called)
     */
    public String getSTDERR() {
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
