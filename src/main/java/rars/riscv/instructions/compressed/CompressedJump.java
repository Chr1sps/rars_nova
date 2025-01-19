package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.BasicInstruction;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimullationCallback;
import rars.util.BinaryUtils;
import rars.util.Utils;

public final class CompressedJump extends CompressedInstruction {
    public static final @NotNull CompressedJump CJ = new CompressedJump(
        "c.j target",
        "Compressed jump: Jump to statement at target address",
        0b101,
        (statement, context) -> Utils.processJump(
            context.registerFile()
                .getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + statement.getOperand(0),
            context.registerFile()
        )
    );

    public static final @NotNull CompressedJump CJAL = new CompressedJump(
        "c.jal offset",
        "Compressed jump and link: Set t1 to Program Counter (return address) "
            + "then jump to statement at target address",
        0b001,
        (statement, context) -> {
            Utils.processReturnAddress(1, context.registerFile()); // x1 is the link register
            Utils.processJump(
                context.registerFile()
                    .getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + statement.getOperand(
                    0), context.registerFile()
            );
        }
    );

    private CompressedJump(
        final @NotNull String example,
        final @NotNull String description,
        final int funct3,
        final @NotNull SimullationCallback callback
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
