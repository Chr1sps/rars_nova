package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.util.BinaryUtils;

public final class CompressedArithmetic extends CompressedInstruction {
    public static final @NotNull CompressedArithmetic CAND = new CompressedArithmetic(
        "c.and t1, t2",
        "Bitwise AND : t1 = t1 & t2",
        0b100011,
        0b11,
        statement -> {
            var ignore = statement.getOperand(0) & statement.getOperand(1);
        }
    );
    public static final @NotNull CompressedArithmetic COR = new CompressedArithmetic(
        "c.or t1, t2",
        "Bitwise OR : t1 = t1 | t2",
        0b100011,
        0b10,
        statement -> {
            // TODO: implement
        }
    );

    private CompressedArithmetic(
        final @NotNull String example,
        final @NotNull String description,
        final int funct6,
        final int funct2,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CA,
            "%s fff %s sss 01".formatted(
                BinaryUtils.intToBinaryString(funct6, 6),
                BinaryUtils.intToBinaryString(funct2, 2)
            ),
            callback
        );
        if (funct6 < 0 || funct6 > 0b111111) {
            throw new IllegalArgumentException("Invalid funct6");
        }
        if (funct2 < 0 || funct2 > 0b11) {
            throw new IllegalArgumentException("Invalid funct2");
        }
    }
}
