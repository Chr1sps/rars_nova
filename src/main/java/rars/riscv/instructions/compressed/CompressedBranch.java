package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;
import rars.util.Utils;

public final class CompressedBranch extends CompressedInstruction {
    public static final @NotNull CompressedBranch BEQZ = new CompressedBranch(
        "c.beqz t1, label",
        "Branch if equal to zero : Branch to statement at label's address if t1 is equal to zero",
        0b000,
        statement -> RegisterFile.getValueLong(statement.getOperand(0)) == 0
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
            statement -> {
                if (callback.doesBranch(statement)) {
                    Utils.processBranch(statement.getOperand(1));
                }
            }
        );
        if (funct3 < 0 || funct3 > 0b111) {
            throw new IllegalArgumentException("Invalid funct3");
        }
    }

    private interface BranchCondition {
        boolean doesBranch(final @NotNull ProgramStatement statement);
    }
}
