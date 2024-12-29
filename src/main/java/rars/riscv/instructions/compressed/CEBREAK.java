package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.BreakpointException;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CEBREAK extends CompressedInstruction {
    public static final @NotNull CEBREAK INSTANCE = new CEBREAK();
    
    private CEBREAK() {
        super("c.ebreak", "Pause execution", CompressedInstructionFormat.CB_FORMAT, "100 1 00000 00000 10");
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        throw BreakpointException.INSTANCE;
    }
}
