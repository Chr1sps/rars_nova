package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.assembler.TokenList;
import rars.exceptions.SimulationException;

public final class ExtendedInstructionNew implements InstructionNew {
    @Override
    public @NotNull String getName() {
        return "";
    }

    @Override
    public int getInstructionLength() {
        return 0;
    }

    @Override
    public @NotNull TokenList getTokenList() {
        return null;
    }

    @Override
    public @NotNull String getDescription() {
        return "";
    }

    @Override
    public @NotNull String getExampleFormat() {
        return "";
    }

    @Override
    public void simulate(final ProgramStatement statement) throws SimulationException {

    }
}
