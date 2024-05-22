package io.github.chr1sps.rars.exceptions;

import io.github.chr1sps.rars.ProgramStatement;

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
     * @param statement a {@link io.github.chr1sps.rars.ProgramStatement} object
     * @param message   a {@link java.lang.String} object
     */
    public ExitingException(ProgramStatement statement, String message) {
        super(statement, message);
    }

    /**
     * <p>Constructor for ExitingException.</p>
     *
     * @param ps  a {@link io.github.chr1sps.rars.ProgramStatement} object
     * @param aee a {@link AddressErrorException} object
     */
    public ExitingException(ProgramStatement ps, AddressErrorException aee) {
        super(ps, aee);
    }
}
