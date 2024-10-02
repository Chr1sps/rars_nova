package rars.api;

import rars.ErrorList;
import rars.Globals;
import rars.RISCVprogram;
import rars.Settings;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.hardware.*;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;
import rars.util.SystemIO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

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
public class Program {

    private final Options set;
    private final RISCVprogram code;
    private final Memory assembled;
    private final Memory simulation;
    private SystemIO.Data fds;
    private ByteArrayOutputStream stdout, stderr;
    private int startPC, exitCode;

    /**
     * <p>Constructor for Program.</p>
     *
     * @param set a {@link Options} object
     */
    public Program(final Options set) {
        Globals.initialize();
        this.set = set;
        this.code = new RISCVprogram();
        this.assembled = new Memory();
        this.simulation = new Memory();
    }

    /**
     * Gets the value of a normal, floating-point or control and status register.
     *
     * @param name Either the common usage (t0, a0, ft0), explicit numbering (x2,
     *             x3, f0), or CSR name (ustatus)
     * @return The value of the register as an int (floats are encoded as IEEE-754)
     * @throws java.lang.NullPointerException if name is invalid; only needs to be checked if
     *                                        code accesses arbitrary names
     */
    public static int getRegisterValue(final String name) {
        Register r = RegisterFile.getRegister(name);
        if (r == null) {
            r = FloatingPointRegisterFile.getRegister(name);
        }
        if (r == null) {
            return ControlAndStatusRegisterFile.getValue(name);
        } else {
            return (int) r.getValue();
        }
    }

    /**
     * Sets the value of a normal, floating-point or control and status register.
     *
     * @param name  Either the common usage (t0, a0, ft0), explicit numbering (x2,
     *              x3, f0), or CSR name (ustatus)
     * @param value The value of the register as an int (floats are encoded as
     *              IEEE-754)
     * @throws java.lang.NullPointerException if name is invalid; only needs to be checked if
     *                                        code accesses arbitrary names
     */
    public static void setRegisterValue(final String name, final int value) {
        Register r = RegisterFile.getRegister(name);
        if (r == null) {
            r = FloatingPointRegisterFile.getRegister(name);
        }
        if (r == null) {
            ControlAndStatusRegisterFile.updateRegister(name, value);
        } else {
            r.setValue(value);
        }
    }

    /**
     * Assembles from a list of files
     *
     * @param files A list of files to assemble
     * @param main  Which file should be considered the main file; it should be in
     *              files
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException thrown if any errors are found in the code
     */
    public ErrorList assemble(final ArrayList<String> files, final String main) throws AssemblyException {
        final ArrayList<RISCVprogram> programs = this.code.prepareFilesForAssembly(files, main, null);
        return this.assemble(programs);
    }

    /**
     * Assembles a single file
     *
     * @param file path to the file to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException thrown if any errors are found in the code
     */
    public ErrorList assemble(final String file) throws AssemblyException {
        // TODO: potentially inline prepareForAssembly
        final ArrayList<String> files = new ArrayList<>();
        files.add(file);
        final ArrayList<RISCVprogram> programs = this.code.prepareFilesForAssembly(files, file, null);
        return this.assemble(programs);
    }

    /**
     * Assembles a string as RISC-V source code
     *
     * @param source the code to assemble
     * @return A list of warnings generated if Options.warningsAreErrors is true,
     * this will be empty
     * @throws AssemblyException thrown if any errors are found in the code
     */
    public ErrorList assembleString(final String source) throws AssemblyException {
        final ArrayList<RISCVprogram> programs = new ArrayList<>();
        this.code.fromString(source);
        this.code.tokenize();
        programs.add(this.code);
        return this.assemble(programs);
    }

    private ErrorList assemble(final ArrayList<RISCVprogram> programs) throws AssemblyException {
        final Memory temp = Memory.swapInstance(this.assembled); // Assembling changes memory so we need to swap to capture that.
        ErrorList warnings = null;
        AssemblyException e = null;
        try {
            warnings = this.code.assemble(programs, this.set.pseudo, this.set.warningsAreErrors);
        } catch (final AssemblyException ae) {
            e = ae;
        }
        Memory.swapInstance(temp);
        if (e != null)
            throw e;

        RegisterFile.initializeProgramCounter(this.set.startAtMain);
        this.startPC = RegisterFile.getProgramCounter();

        return warnings;
    }

    /**
     * Prepares the simulator for execution. Clears registers, loads arguments
     * into memory and initializes the String backed STDIO
     *
     * @param args  Just like the args to a Java main, but an ArrayList.
     * @param STDIN A string that can be read in the program like its stdin or null
     *              to allow IO passthrough
     */
    public void setup(final ArrayList<String> args, final String STDIN) {
        RegisterFile.resetRegisters();
        FloatingPointRegisterFile.resetRegisters();
        ControlAndStatusRegisterFile.resetRegisters();
        InterruptController.reset();
        RegisterFile.initializeProgramCounter(this.startPC);
        Globals.exitCode = 0;

        // Copy in assembled code and arguments
        this.simulation.copyFrom(this.assembled);
        final Memory tmpMem = Memory.swapInstance(this.simulation);
        new ProgramArgumentList(args).storeProgramArguments();
        Memory.swapInstance(tmpMem);

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
     * @throws SimulationException thrown if there is an uncaught interrupt. The
     *                             program cannot be simulated further.
     */
    public Simulator.Reason simulate() throws SimulationException {
        Simulator.Reason ret = null;
        SimulationException e = null;

        // Swap out global state for local state.
        final boolean selfMod = Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED,
                this.set.selfModifyingCode);
        final SystemIO.Data tmpFiles = SystemIO.swapData(this.fds);
        final Memory tmpMem = Memory.swapInstance(this.simulation);

        try {
            ret = RISCVprogram.simulate(this.set.maxSteps);
        } catch (final SimulationException se) {
            e = se;
        }
        this.exitCode = Globals.exitCode;

        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, selfMod);
        SystemIO.swapData(tmpFiles);
        Memory.swapInstance(tmpMem);

        if (e != null)
            throw e;
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
}
