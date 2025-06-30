package rars.exceptions

import rars.runtime.ExceptionReason

/** Represents an exception that occurs during the simulation. */
sealed interface SimulationException

/** Represents an exception that occurs when the simulation encounters a breakpoint. */
data object BreakpointException : SimulationException

/** Represents an exception that signals the simulation to wait for an interrupt. */
data object WaitException : SimulationException

/** Represents a successful exit from the program. */
data object ExitingException : SimulationException

sealed interface SimulationError : SimulationException {
    val reason: ExceptionReason
    val value: Int
}

data class ExitingError(
    override val reason: ExceptionReason,
    val message: Any, // TODO: change to a possible error message class
    override val value: Int
) : SimulationError 