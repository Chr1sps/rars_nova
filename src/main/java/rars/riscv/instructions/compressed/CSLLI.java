package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CSLLI extends CompressedInstruction {
    public static final @NotNull CSLLI INSTANCE = new CSLLI();

    private CSLLI() {
        super(
            "c.slli t1, 100",
            "",
            CompressedInstructionFormat.CI,
            "000  10"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        // TODO: implement
    }
}
