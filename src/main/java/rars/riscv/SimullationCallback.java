package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

public interface SimullationCallback {
    void simulate(@NotNull ProgramStatement statement, @NotNull SimulationContext context) throws SimulationException;
}
