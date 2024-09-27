package rars.exceptions;

import rars.ProgramStatement;

/**
 * Exceptions that cannot be handled and must result in the ending of the
 * simulation
 * <p>
 * Used for exit syscalls and errors in syscalls
 *
 */
public class ExitingException extends SimulationException {
    /**
     * <p>Constructor for ExitingException.</p>
     */
    public ExitingException() {
        super();
    }

    /**
     * <p>Constructor for ExitingException.</p>
     *
     * @param statement a {@link ProgramStatement} object
     * @param message   a {@link java.lang.String} object
     */
    public ExitingException(final ProgramStatement statement, final String message) {
        super(statement, message);
    }

    /**
     * <p>Constructor for ExitingException.</p>
     *
     * @param ps  a {@link ProgramStatement} object
     * @param aee a {@link AddressErrorException} object
     */
    public ExitingException(final ProgramStatement ps, final AddressErrorException aee) {
        super(ps, aee);
    }
}
