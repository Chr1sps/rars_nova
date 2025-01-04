package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;

public final class CADDI4SPN extends CompressedInstruction {
    public static final @NotNull CADDI4SPN INSTANCE = new CADDI4SPN();

    private CADDI4SPN() {
        super(
            "c.addi4spn t1, 100",
            "Adds a zero-extended non-zero scaled by 4 to the stack pointer and saves the result to t1",
            CompressedInstructionFormat.CIW,
            "000 ssssssss fff 00",
            operands -> {
                // TODO: implement
            }
        );
    }

}
