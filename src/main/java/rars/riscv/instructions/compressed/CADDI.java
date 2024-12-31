package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CADDI extends CompressedInstruction {
    public static final @NotNull CADDI CADDI = new CADDI();

    private CADDI() {
        super(
            "c.addi t1, 100",
            "Add immediate to register",
            CompressedInstructionFormat.CI,
            "000 s fffff sssss 01",
            statement -> {
                // TODO: implement
            }
        );
    }
}
