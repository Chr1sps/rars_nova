package rars.events

import rars.ErrorMessage
import rars.Globals
import rars.ProgramStatement
import rars.riscv.BasicInstruction
import rars.util.toHexStringWithPrefix


/** Represents an event that occurs during the simulation. */
sealed interface SimulationEvent

/** Represents and event that occurs when the simulation encounters a breakpoint. */
object BreakpointEvent : SimulationEvent

/** Represents an event that signals the simulation to wait for an interrupt. */
object WaitEvent : SimulationEvent

/** Represents a successful exit from the program. */
object ExitingEvent : SimulationEvent

/** Represents an error that occurred during the simulation. */
sealed interface SimulationError : SimulationEvent {
    val reason: EventReason
    val message: ErrorMessage
    val value: Int

    companion object {
        @JvmStatic
        fun create(
            reason: EventReason,
            message: ErrorMessage,
            value: Int,
        ): SimulationError = SimulationErrorImpl(reason, message, value)

        @JvmStatic
        fun create(message: String, reason: EventReason) = create(
            reason,
            ErrorMessage.error(null, 0, 0, message),
            0
        )

        @JvmStatic
        fun create(statement: ProgramStatement, message: String, reason: EventReason): SimulationError {
            val address = Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
            return create(
                reason,
                ErrorMessage.error(
                    statement.sourceProgram,
                    statement.sourceLine!!.lineNumber,
                    0,
                    "Runtime exception at ${address.toHexStringWithPrefix()}: $message"
                ),
                0
            )
        }

        @JvmStatic
        fun create(statement: ProgramStatement, memoryError: MemoryError): SimulationError {
            val address = Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
            return create(
                memoryError.reason,
                ErrorMessage.error(
                    statement.sourceProgram,
                    statement.sourceLine!!.lineNumber,
                    0,
                    "Runtime exception at ${address.toHexStringWithPrefix()}: ${memoryError.message}"
                ),
                memoryError.address
            )
        }
    }

    private class SimulationErrorImpl(
        override val reason: EventReason,
        override val message: ErrorMessage,
        override val value: Int,
    ) : SimulationError
}

/** Represents an exit from the program due to an error. */
class ExitingError(
    override val reason: EventReason,
    override val message: ErrorMessage,
    override val value: Int,
) : SimulationError {
    companion object {
        operator fun invoke(
            statement: ProgramStatement,
            memoryError: MemoryError,
        ): ExitingError {
            val address =
                (Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH).toHexStringWithPrefix()
            return ExitingError(
                memoryError.reason,
                ErrorMessage.error(
                    statement.sourceProgram,
                    statement.sourceLine!!.lineNumber,
                    0,
                    "Runtime exception at $address: ${memoryError.message}"
                ),
                memoryError.address
            )
        }

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

