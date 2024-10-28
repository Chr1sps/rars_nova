package rars.exceptions;

/**
 * This exception is only used to trigger breakpoints for ebreak.
 * <p>
 * Its a bit of a hack, but it works and somewhat makes logical sense.
 */
public final class BreakpointException extends SimulationException {
    public static final BreakpointException INSTANCE = new BreakpointException();

    private BreakpointException() {
    }
}
