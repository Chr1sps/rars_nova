package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.util.BinaryUtils;

import java.util.List;

public final class CompressedStore extends CompressedInstruction {
    public static final @NotNull CompressedStore CFSD = new CompressedStore(
        "c.fsd f1, -100(t1)",
        "Store a double to memory",
        0b101,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStore CFSW = new CompressedStore(
        "c.fsw f1, -100(t1)",
        "Store a float to memory",
        0b111,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStore CSW = new CompressedStore(
        "c.sw t1, -100(t2)",
        "Store word : Store contents of t1 into effective memory word address",
        0b110,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedStore CSD = new CompressedStore(
        "c.sd t1, -100(t2)",
        "Store double word : Store contents of t1 into effective memory double word address",
        0b111,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull
    @Unmodifiable List<@NotNull CompressedStore> ALL_INSTRUCTIONS = List.of(
        CFSD,
        CFSW,
        CSW,
        CSD
    );

    private CompressedStore(
        final @NotNull String example,
        final @NotNull String description,
        final int opcode,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CS,
            BinaryUtils.intToBinaryString(opcode, 3) + " sss ttt ss fff 00",
            callback
        );
        if (opcode < 0 || opcode > 0b111) {
            throw new IllegalArgumentException("Invalid opcode");
        }
    }
}
