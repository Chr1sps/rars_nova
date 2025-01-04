package rars.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public final @NotNull ExceptionReason reason;
    public final int value;
    public final @Nullable ErrorMessage errorMessage;

    private SimulationException(final ExceptionReason reason, final @Nullable ErrorMessage msg, final int value) {
        this.reason = reason;
        this.value = value;
        this.errorMessage = msg;
    }

    public SimulationException(final String m, final ExceptionReason reason) {
        this(reason, ErrorMessage.error(null, 0, 0, m), 0);
    }
    
    public SimulationException() {
        this(ExceptionReason.OTHER, null, 0);
    }

    public SimulationException(final ProgramStatement ps, final String m, final ExceptionReason reason) {
        this(reason, new ErrorMessage(
                ps,
                "Runtime exception at %s: %s"
                        .formatted(
                                BinaryUtils.intToHexString(
                                        RegisterFile.getProgramCounter()
                                                - BasicInstruction.BASIC_INSTRUCTION_LENGTH),
                                m
                        )), 0);
    }

    public SimulationException(final ProgramStatement ps, final String m) {
        this(ps, m, ExceptionReason.OTHER);
    }

    public SimulationException(final @NotNull ProgramStatement ps, final @NotNull AddressErrorException aee) {
        this(
                aee.reason, new ErrorMessage(
                        ps, "Runtime exception at " +
                        BinaryUtils.intToHexString(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH) +
                        ": " + aee.getMessage()
                ), aee.address
        );
    }

    public SimulationException(final String m) {
        this(m, ExceptionReason.OTHER);
    }

    public static @NotNull SimulationException fromAddressErrorException(final ProgramStatement ps, final AddressErrorException aee) {
        return new SimulationException(ps, aee);
    }
}