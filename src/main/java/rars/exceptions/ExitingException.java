package rars.exceptions;

import rars.ProgramStatement;

/**
 * Exceptions that cannot be handled and must result in the ending of the
 * simulation
 * <p>
 * Used for exit syscalls and errors in syscalls
 */
public final class ExitingException extends SimulationException {
    public ExitingException() {
        super();
    }

    public ExitingException(final ProgramStatement statement, final String message) {
        super(statement, message);
    }

    public ExitingException(final ProgramStatement ps, final AddressErrorException aee) {
        super(ps, aee);
    }
}
