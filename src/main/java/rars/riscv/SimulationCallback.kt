package rars.riscv

import arrow.core.Either
import rars.ProgramStatement
import rars.exceptions.SimulationEvent
import rars.simulator.SimulationContext

/**
 * A functional interface that represents a callback to simulate
 * in RARS given the current machine statement and context.
 */
fun interface SimulationCallback {
    /**
     * Executes the callback.
     *
     * @param statement
     * A ProgramStatement representing the MIPS instruction to
     * simulate.
     * @param context
     * The context in which the simulation is taking place.
     * @throws SimulationException
     * This is a run-time exception generated during
     * simulation.
     */
    fun SimulationContext.simulate(statement: ProgramStatement): Either<SimulationEvent, Unit>
}

typealias SimulationCallbackFunc = SimulationContext.(ProgramStatement) -> Either<SimulationEvent, Unit>