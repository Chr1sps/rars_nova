package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.BreakpointException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CEBREAK extends CompressedInstruction {
    public static final @NotNull CEBREAK INSTANCE = new CEBREAK();

    private CEBREAK() {
        super(
            "c.ebreak",
            "Pause execution",
            CompressedInstructionFormat.CB,
            "100 1 00000 00000 10",
            (statement, context) -> {
                throw BreakpointException.INSTANCE;
            }
        );
    }
}
