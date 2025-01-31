package rars.riscv

import rars.ProgramStatement
import rars.exceptions.SimulationException
import rars.simulator.SimulationContext

/**
 * A functional interface that represents a callback to simulate
 * in RARS given the current machine statement and context.
 */
fun interface SimulationCallback {
//    @Throws(SimulationException::class)
//    fun simulate(context: SimulationContext, statement: ProgramStatement) {
//        context.simulateImpl(statement)
//    }

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
    @Throws(SimulationException::class)
    fun SimulationContext.simulateImpl(statement: ProgramStatement)
}
