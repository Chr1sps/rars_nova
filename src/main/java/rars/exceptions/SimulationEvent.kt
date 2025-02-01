package rars.exceptions

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
    val reason: ExceptionReason
    val message: ErrorMessage
    val value: Int

    companion object {
        @JvmStatic
        fun create(
            reason: ExceptionReason,
            message: ErrorMessage,
            value: Int,
        ): SimulationError = SimulationErrorImpl(reason, message, value)

        @JvmStatic
        fun create(message: String, reason: ExceptionReason) = create(
            reason,
            ErrorMessage.error(null, 0, 0, message),
            0
        )

        @JvmStatic
        fun create(statement: ProgramStatement, message: String, reason: ExceptionReason): SimulationError {
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
        fun create(statement: ProgramStatement, addressError: AddressErrorException): SimulationError {
            val address = Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
            return create(
                addressError.reason,
                ErrorMessage.error(
                    statement.sourceProgram,
                    statement.sourceLine!!.lineNumber,
                    0,
                    "Runtime exception at ${address.toHexStringWithPrefix()}: ${addressError.message}"
                ),
                addressError.address
            )
        }
    }

    private class SimulationErrorImpl(
        override val reason: ExceptionReason,
        override val message: ErrorMessage,
        override val value: Int,
    ) : SimulationError
}

/** Represents an exit from the program due to an error. */
class ExitingError(
    override val reason: ExceptionReason,
    override val message: ErrorMessage,
    override val value: Int,
) : SimulationError

