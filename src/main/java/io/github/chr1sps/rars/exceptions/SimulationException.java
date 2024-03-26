package io.github.chr1sps.rars.exceptions;

import io.github.chr1sps.rars.ErrorMessage;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.Instruction;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import io.github.chr1sps.rars.util.Binary;

/**
 * For exceptions thrown during runtime
 * <p>
 * if cause is -1, the exception is not-handlable is user code.
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SimulationException extends Exception {

    // Interrupts
    /**
     * Constant <code>SOFTWARE_INTERRUPT=0x80000000</code>
     */
    public static final int SOFTWARE_INTERRUPT = 0x80000000;
    /**
     * Constant <code>TIMER_INTERRUPT=0x80000004</code>
     */
    public static final int TIMER_INTERRUPT = 0x80000004;
    /**
     * Constant <code>EXTERNAL_INTERRUPT=0x80000008</code>
     */
    public static final int EXTERNAL_INTERRUPT = 0x80000008;
    // Traps
    /**
     * Constant <code>INSTRUCTION_ADDR_MISALIGNED=0</code>
     */
    public static final int INSTRUCTION_ADDR_MISALIGNED = 0;
    /**
     * Constant <code>INSTRUCTION_ACCESS_FAULT=1</code>
     */
    public static final int INSTRUCTION_ACCESS_FAULT = 1;
    /**
     * Constant <code>ILLEGAL_INSTRUCTION=2</code>
     */
    public static final int ILLEGAL_INSTRUCTION = 2;
    /**
     * Constant <code>LOAD_ADDRESS_MISALIGNED=4</code>
     */
    public static final int LOAD_ADDRESS_MISALIGNED = 4;
    /**
     * Constant <code>LOAD_ACCESS_FAULT=5</code>
     */
    public static final int LOAD_ACCESS_FAULT = 5;
    /**
     * Constant <code>STORE_ADDRESS_MISALIGNED=6</code>
     */
    public static final int STORE_ADDRESS_MISALIGNED = 6;
    /**
     * Constant <code>STORE_ACCESS_FAULT=7</code>
     */
    public static final int STORE_ACCESS_FAULT = 7;
    /**
     * Constant <code>ENVIRONMENT_CALL=8</code>
     */
    public static final int ENVIRONMENT_CALL = 8;

    private int cause = -1, value = 0;
    private ErrorMessage message = null;

    /**
     * <p>Constructor for SimulationException.</p>
     */
    public SimulationException() {
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param ps    a {@link io.github.chr1sps.rars.ProgramStatement} object
     * @param m     a {@link java.lang.String} object
     * @param cause a int
     */
    public SimulationException(ProgramStatement ps, String m, int cause) {
        this(ps, m);
        this.cause = cause;
    }

    /**
     * Constructor for ProcessingException to handle runtime exceptions
     *
     * @param ps a ProgramStatement of statement causing runtime exception
     * @param m  a String containing specialized error message
     */
    public SimulationException(ProgramStatement ps, String m) {
        message = new ErrorMessage(ps, "Runtime exception at " +
                Binary.intToHexString(RegisterFile.getProgramCounter() - Instruction.INSTRUCTION_LENGTH) +
                ": " + m);
        // Stopped using ps.getAddress() because of pseudo-instructions. All
        // instructions in
        // the macro expansion point to the same ProgramStatement, and thus all will
        // return the
        // same value for getAddress(). But only the first such expanded instruction
        // will
        // be stored at that address. So now I use the program counter (which has
        // already
        // been incremented).
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param ps  a {@link io.github.chr1sps.rars.ProgramStatement} object
     * @param aee a {@link AddressErrorException} object
     */
    public SimulationException(ProgramStatement ps, AddressErrorException aee) {
        this(ps, aee.getMessage());
        cause = aee.getType();
        value = aee.getAddress();
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param m a {@link java.lang.String} object
     */
    public SimulationException(String m) {
        message = new ErrorMessage(null, 0, 0, m);
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param m     a {@link java.lang.String} object
     * @param cause a int
     */
    public SimulationException(String m, int cause) {
        message = new ErrorMessage(null, 0, 0, m);
        this.cause = cause;
    }

    /**
     * Produce the list of error messages.
     *
     * @return Returns the Message associated with the exception
     * @see ErrorMessage
     */
    public ErrorMessage error() {
        return message;
    }

    /**
     * <p>cause.</p>
     *
     * @return a int
     */
    public int cause() {
        return cause;
    }

    /**
     * <p>value.</p>
     *
     * @return a int
     */
    public int value() {
        return value;
    }
}
