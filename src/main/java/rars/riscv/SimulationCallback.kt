package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.simulator.SimulationContext;

/**
 * A functional interface that represents a callback to simulate
 * in RARS given the current machine statement and context.
 */
@FunctionalInterface
public interface SimulationCallback {
    /**
     * Executes the callback.
     *
     * @param statement
     *     A ProgramStatement representing the MIPS instruction to
     *     simulate.
     * @param context
     *     The context in which the simulation is taking place.
     * @throws SimulationException
     *     This is a run-time exception generated during
     *     simulation.
     */
    void simulate(@NotNull ProgramStatement statement, @NotNull SimulationContext context) throws SimulationException;
}
