package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

import static rars.exceptions.BreakpointException.BREAKPOINT_EXCEPTION;

public final class CEBREAK extends CompressedInstruction {
    public static final @NotNull CEBREAK CEBREAK = new CEBREAK();

    private CEBREAK() {
        super(
            "c.ebreak",
            "Pause execution",
            CompressedInstructionFormat.CB,
            "100 1 00000 00000 10",
            operands -> {
                throw BREAKPOINT_EXCEPTION;
            }
        );
    }
}
