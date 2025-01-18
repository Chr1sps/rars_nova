package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationContext;
import rars.util.BinaryUtils;
import rars.util.Utils;

public final class CompressedBranch extends CompressedInstruction {
    public static final @NotNull CompressedBranch CBEQZ = new CompressedBranch(
        "c.beqz t1, label",
        "Branch if equal to zero : Branch to statement at label's address if t1 is equal to zero",
        0b110,
        (statement, context) -> context.registerFile().getLongValue(statement.getOperand(0)) == 0
    );
    public static final @NotNull CompressedBranch CBNEZ = new CompressedBranch(
        "c.bnez t1, label",
        "Branch if not equal to zero : Branch to statement at label's address if t1 is not equal to zero",
        0b111,
        (statement, context) -> context.registerFile().getLongValue(statement.getOperand(0)) != 0
    );

    private CompressedBranch(
        final @NotNull String example,
        final @NotNull String description,
        final int funct3,
        final @NotNull CompressedBranch.BranchCondition callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CB,
            "%s sss fff sssss 01".formatted(
                BinaryUtils.intToBinaryString(funct3, 3)
            ),
            (statement, context) -> {
                if (callback.doesBranch(statement, context)) {
                    Utils.processBranch(statement.getOperand(1));
                }
            }
        );
        if (funct3 < 0 || funct3 > 0b111) {
            throw new IllegalArgumentException("Invalid funct3");
        }
    }

    private interface BranchCondition {
        boolean doesBranch(final @NotNull ProgramStatement statement, final @NotNull SimulationContext context);
    }
}
