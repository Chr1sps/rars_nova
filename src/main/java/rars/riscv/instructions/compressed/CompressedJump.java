package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.BasicInstruction;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;
import rars.util.Utils;

public final class CompressedJump extends CompressedInstruction {
    public static final @NotNull CompressedJump CJ = new CompressedJump(
        "c.j target",
        "Compressed jump: Jump to statement at target address",
        0b101,
        statement -> Utils.processJump(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + statement.getOperand(
            0))
    );

    public static final @NotNull CompressedJump CJAL = new CompressedJump(
        "c.jal offset",
        "Compressed jump and link: Set t1 to Program Counter (return address) "
            + "then jump to statement at target address",
        0b001,
        statement -> {
            Utils.processReturnAddress(1); // x1 is the link register
            Utils.processJump(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + statement.getOperand(
                0));
        }
    );

    private CompressedJump(
        final @NotNull String example,
        final @NotNull String description,
        final int funct3,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CJ,
            "%s fffffffffff 01".formatted(
                BinaryUtils.intToBinaryString(funct3, 3)
            ),
            callback
        );
    }
}
