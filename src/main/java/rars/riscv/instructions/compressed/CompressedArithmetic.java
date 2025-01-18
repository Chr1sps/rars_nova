package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.InstructionsRegistry;
import rars.util.BinaryUtils;

public final class CompressedArithmetic extends CompressedInstruction {
    public static final @NotNull CompressedArithmetic CAND = new CompressedArithmetic(
        "c.and t1, t2",
        "Bitwise AND : t1 = t1 & t2",
        0b100011,
        0b11,
        (first, second) -> first & second
    );
    public static final @NotNull CompressedArithmetic COR = new CompressedArithmetic(
        "c.or t1, t2",
        "Bitwise OR : t1 = t1 | t2",
        0b100011,
        0b10,
        (first, second) -> first | second
    );
    public static final @NotNull CompressedArithmetic CXOR = new CompressedArithmetic(
        "c.xor t1, t2",
        "Bitwise XOR : t1 = t1 ^ t2",
        0b100011,
        0b01,
        (first, second) -> first ^ second
    );
    public static final @NotNull CompressedArithmetic CSUB = new CompressedArithmetic(
        "c.sub t1, t2",
        "Subtraction : t1 = t1 - t2",
        0b100011,
        0b00,
        (first, second) -> first - second
    );

    private CompressedArithmetic(
        final @NotNull String example,
        final @NotNull String description,
        final int funct6,
        final int funct2,
        final @NotNull Computation callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CA,
            "%s fff %s sss 01".formatted(
                BinaryUtils.intToBinaryString(funct6, 6),
                BinaryUtils.intToBinaryString(funct2, 2)
            ),
            (statement, context) -> {
                if (InstructionsRegistry.RV64_MODE_FLAG) {
                    final long newValue = callback.computeDoubleWord(
                        context.registerFile().getLongValue(statement.getOperand(0)),
                        context.registerFile().getLongValue(statement.getOperand(1))
                    );
                    context.registerFile().updateRegisterByNumber(
                        statement.getOperand(statement.getOperand(0)),
                        newValue
                    );
                } else {
                    final long newValue = callback.computeWord(
                        context.registerFile().getIntValue(statement.getOperand(0)),
                        context.registerFile().getIntValue(statement.getOperand(1))
                    );
                    context.registerFile().updateRegisterByNumber(
                        statement.getOperand(statement.getOperand(0)),
                        newValue
                    );
                }
            }
        );
        if (funct6 < 0 || funct6 > 0b111111) {
            throw new IllegalArgumentException("Invalid funct6");
        }
        if (funct2 < 0 || funct2 > 0b11) {
            throw new IllegalArgumentException("Invalid funct2");
        }
    }

    @FunctionalInterface
    private interface Computation {
        long computeDoubleWord(final long t1, final long t2);

        default int computeWord(final int t1, final int t2) {
            return (int) computeDoubleWord(t1, t2);
        }
    }
}
