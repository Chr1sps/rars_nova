package rars.events

import rars.ErrorMessage
import rars.ProgramStatement


/** Represents an event that occurs during the simulation. */
sealed interface SimulationEvent

/** Represents and event that occurs when the simulation encounters a breakpoint. */
data object BreakpointEvent : SimulationEvent

/** Represents an event that signals the simulation to wait for an interrupt. */
data object WaitEvent : SimulationEvent

/** Represents a successful exit from the program. */
data object ExitingEvent : SimulationEvent

/** Represents an error that occurred during the simulation. */
sealed interface SimulationError : SimulationEvent {
    val reason: EventReason
    val message: ErrorMessage
    val value: Int

    companion object {
        fun create(
            reason: EventReason,
            message: ErrorMessage,
            value: Int,
        ): SimulationError = SimulationErrorImpl(reason, message, value)

        fun create(message: String, reason: EventReason) = create(
            reason,
            ErrorMessage.error(null, 0, 0, message),
            0
        )

    }
}

private data class SimulationErrorImpl(
    override val reason: EventReason,
    override val message: ErrorMessage,
    override val value: Int,
) : SimulationError

/** Represents an exit from the program due to an error. */
data class ExitingError(
    override val reason: EventReason,
    override val message: ErrorMessage,
    override val value: Int,
) : SimulationError {
    companion object {
        operator fun invoke(
            statement: ProgramStatement,
            message: String,
        ) = ExitingError(
            EventReason.OTHER,
            ErrorMessage.error(
                statement.sourceProgram,
                statement.sourceLine!!.lineNumber,
                0,
                message
            ),
            0
        )
    }
}

