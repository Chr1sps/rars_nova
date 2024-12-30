package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

public interface SimulationCallback {
    void simulate(final @NotNull ProgramStatement statement) throws SimulationException;
}
