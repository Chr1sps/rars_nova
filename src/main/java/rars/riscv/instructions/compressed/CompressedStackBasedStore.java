package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.util.BinaryUtils;

public final class CompressedStackBasedStore extends CompressedInstruction {
    public static final @NotNull CompressedStackBasedStore CFSDSP = new CompressedStackBasedStore(
        "c.fsdsp f1, -100(sp)",
        "Store a floating point double from f1 to memory at address relative to the stack pointer",
        0b101,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStackBasedStore CSWSP = new CompressedStackBasedStore(
        "c.swsp t1, -100(sp)",
        "Store word from t1 to memory at address relative to the stack pointer",
        0b110,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStackBasedStore CSDSP = new CompressedStackBasedStore(
        "c.sdsp t1, -100(sp)",
        "Store a double word from t1 to memory at address relative to the stack pointer",
        0b111,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStackBasedStore CFSWSP = new CompressedStackBasedStore(
        "c.fswsp f1, -100(sp)",
        "Store a floating point word from f1 to memory at address relative to the stack pointer",
        0b111,
        statement -> {
            // TODO: implement
        }
    );

    private CompressedStackBasedStore(
        final @NotNull String example,
        final @NotNull String description,
        final int opcode,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CL,
            BinaryUtils.intToBinaryString(opcode, 3) + "ssssss fffff 10",
            callback
        );
        if (opcode < 0 || opcode > 0b111) {
            throw new IllegalArgumentException("Invalid opcode");
        }
    }
}
