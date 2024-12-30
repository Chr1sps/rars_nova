package rars.exceptions;

import rars.ErrorMessage;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;

/**
 * For exceptions thrown during runtime
 * <p>
 * if cause is -1, the exception is not-handlable is user code.
 */
public class SimulationException extends Exception {

    public final ExceptionReason reason;
    public final int value;
    public final ErrorMessage errorMessage;


    private SimulationException(final ExceptionReason reason, final ErrorMessage msg, final int value) {
        this.reason = reason;
        this.value = value;
        this.errorMessage = msg;
    }

    /**
     * <p>Constructor for SimulationException.</p>
     */
    public SimulationException() {
        this(ExceptionReason.OTHER, null, 0);
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param ps     a {@link ProgramStatement} object
     * @param m      a {@link java.lang.String} object
     * @param reason a int
     */
    public SimulationException(final ProgramStatement ps, final String m, final ExceptionReason reason) {
        this(reason, new ErrorMessage(ps, "Runtime exception at " +
            BinaryUtils.intToHexString(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH) +
            ": " + m), 0);
    }

    /**
     * Constructor for ProcessingException to handle runtime exceptions
     *
     * @param ps a ProgramStatement of statement causing runtime exception
     * @param m  a String containing specialized error message
     */
    public SimulationException(final ProgramStatement ps, final String m) {
        this(ps, m, ExceptionReason.OTHER);
        // Stopped using ps.getAddress() because of usePseudoInstructions-instructions. All
        // instructions in
        // the macro expansion point to the same ProgramStatement, and thus all will
        // return the
        // same second for getAddress(). But only the first such expanded instruction
        // will
        // be stored at that address. So now I use the program counter (which has
        // already
        // been incremented).
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param ps  a {@link ProgramStatement} object
     * @param aee a {@link AddressErrorException} object
     */
    public SimulationException(final ProgramStatement ps, final AddressErrorException aee) {
        this(aee.reason, new ErrorMessage(ps, "Runtime exception at " +
            BinaryUtils.intToHexString(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH) +
            ": " + aee.getMessage()), aee.address);
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param m a {@link java.lang.String} object
     */
    public SimulationException(final String m) {
        this(m, ExceptionReason.OTHER);
    }

    /**
     * <p>Constructor for SimulationException.</p>
     *
     * @param m      a {@link java.lang.String} object
     * @param reason a int
     */
    public SimulationException(final String m, final ExceptionReason reason) {
        this.errorMessage = ErrorMessage.error(null, 0, 0, m);
        this.reason = reason;
        this.value = 0;
    }
}