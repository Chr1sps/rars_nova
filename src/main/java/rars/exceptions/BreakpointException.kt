package rars.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * This exception is only used to trigger breakpoints for ebreak.
 * <p>
 * It's a bit of a hack, but it works and somewhat makes logical sense.
 */
public final class BreakpointException extends SimulationException {
    public static final @NotNull BreakpointException INSTANCE = new BreakpointException();

    private BreakpointException() {
        super(ExceptionReason.OTHER, null, 0);
    }
}
