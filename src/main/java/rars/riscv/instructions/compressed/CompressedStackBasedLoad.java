package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;

public final class CompressedStackBasedLoad extends CompressedInstruction {
    public static final @NotNull CompressedStackBasedLoad CLWSP = new CompressedStackBasedLoad(
        "c.lwsp t1, -100(sp)",
        "Load word from a given offset from the stack pointer",
        0b010,
        statement -> {
            final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
            try {
                RegisterFile.updateRegister(
                    statement.getOperand(0),
                    Memory.getInstance().getWord(
                        RegisterFile.getValue(RegisterFile.STACK_POINTER_REGISTER_INDEX) + upperImmediate
                    )
                );
            } catch (final AddressErrorException e) {
                throw new SimulationException(statement, e);
            }
        }
    );

    private CompressedStackBasedLoad(
        final @NotNull String example,
        final @NotNull String description,
        final int funct3,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CI,
            "%s s fffff sssss 10".formatted(
                BinaryUtils.intToBinaryString(funct3, 3)
            ),
            callback
        );
        if (funct3 < 0 || funct3 > 0b111) {
            throw new IllegalArgumentException("funct3 must be in range [0, 0b111]");
        }
    }
}
