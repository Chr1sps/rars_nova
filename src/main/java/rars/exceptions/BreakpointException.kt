package rars.exceptions

/**
 * This exception is only used to trigger breakpoints for ebreak.
 *
 *
 * It's a bit of a hack, but it works and somewhat makes logical sense.
 */
object BreakpointException : SimulationException(ExceptionReason.OTHER, null, 0) 
 

