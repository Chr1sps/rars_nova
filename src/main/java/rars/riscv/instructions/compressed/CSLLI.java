package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CSLLI extends CompressedInstruction {
    public static final @NotNull CSLLI INSTANCE = new CSLLI();

    private CSLLI() {
        super(
            "c.slli t1, 100",
            "",
            CompressedInstructionFormat.CI,
            "000 s fffff ssss 10",
            (statement, context) -> {
                // TODO: Implement
            }
        );
    }
}
