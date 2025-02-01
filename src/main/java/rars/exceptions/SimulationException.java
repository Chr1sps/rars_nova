package rars.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorMessage;
import rars.Globals;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.util.BinaryUtilsKt;

/**
 * For exceptions thrown during runtime
 * <p>
 * if cause is -1, the exception is not-handlable is user code.
 */
public class SimulationException extends Exception {

    public final @NotNull ExceptionReason reason;
    public final int value;
    public final @Nullable ErrorMessage errorMessage;

    protected SimulationException(
        final @NotNull ExceptionReason reason,
        final @Nullable ErrorMessage msg,
        final int value
    ) {
        this.reason = reason;
        this.value = value;
        this.errorMessage = msg;
    }

    public SimulationException(final @NotNull String m, final @NotNull ExceptionReason reason) {
        this(reason, ErrorMessage.error(null, 0, 0, m), 0);
    }

    public SimulationException(
        final @NotNull ProgramStatement ps,
        final @NotNull String message,
        final @NotNull ExceptionReason reason
    ) {
        this(
            reason,
            ErrorMessage.error(
                ps.getSourceProgram(),
                ps.sourceLine.lineNumber(),
                0,
                "Runtime exception at %s: %s".formatted(
                    BinaryUtilsKt.intToHexStringWithPrefix(Globals.REGISTER_FILE.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH),
                    message
                )
            ),
            0
        );
    }

    public SimulationException(final @NotNull ProgramStatement ps, final @NotNull AddressErrorException aee) {
        this(
            aee.reason,
            ErrorMessage.error(
                ps.getSourceProgram(),
                ps.sourceLine.lineNumber(),
                0,
                "Runtime exception at %s: %s".formatted(
                    BinaryUtilsKt.intToHexStringWithPrefix(Globals.REGISTER_FILE.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH),
                    aee.getMessage()
                )
            ),
            aee.address
        );
    }
}