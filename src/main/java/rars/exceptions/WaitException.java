package rars.exceptions;

/**
 * Exception to trigger the simulator to wait for an interrupt; used in WFI
 */
public final class WaitException extends SimulationException {
    public static final WaitException INSTANCE = new WaitException();

    private WaitException() {
    }
}
