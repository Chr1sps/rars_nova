package rars.riscv;

import rars.ProgramStatement;
import rars.exceptions.SimulationException;

@FunctionalInterface
public interface SimulationCallback {
    void simulate(ProgramStatement statement) throws SimulationException;
}
