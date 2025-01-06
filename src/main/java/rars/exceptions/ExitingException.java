package rars.exceptions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;

/**
 * Exceptions that cannot be handled and must result in the ending of the
 * simulation
 * <p>
 * Used for exit syscalls and errors in syscalls
 */
public final class ExitingException extends SimulationException {
    public ExitingException() {
        super(ExceptionReason.OTHER, null, 0);
    }

    public ExitingException(final @NotNull ProgramStatement statement, final @NotNull String message) {
        super(statement, message, ExceptionReason.OTHER);
    }

    public ExitingException(final @NotNull ProgramStatement ps, final @NotNull AddressErrorException aee) {
        super(ps, aee);
    }
}
