package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.util.BinaryUtils;

import java.util.List;

public final class CompressedLoad extends CompressedInstruction {
    public static final @NotNull CompressedLoad CLW = new CompressedLoad(
        "c.lw t1, -100(t2)",
        "Set t1 to contents of effective memory word address",
        0b010,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedLoad CFLW = new CompressedLoad(
        "c.flw f1, -100(t1)",
        "Load a float from memory",
        0b001,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedLoad CLD = new CompressedLoad(
        "c.ld t1, -100(t2)",
        "Set t1 to contents of effective memory double word address",
        0b001,
        statement -> {
            // TODO: implement
        }
    );
    public static final @NotNull CompressedLoad CFLD = new CompressedLoad(
        "c.fld f1, -100(t1)",
        "Load a double from memory",
        0b001,
        statement -> {
            // TODO: implement
        }
    );

    public static final @NotNull
    @Unmodifiable List<@NotNull CompressedLoad> ALL_INSTRUCTIONS = List.of(
        CLW,
        CFLW,
        CLD,
        CFLD
    );

    private CompressedLoad(
        final @NotNull String example,
        final @NotNull String description,
        final int opcode,
        final @NotNull SimulationCallback callback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CL,
            BinaryUtils.intToBinaryString(opcode, 3) + " sss ttt ss fff 00",
            callback
        );
        if (opcode < 0 || opcode > 0b111) {
            throw new IllegalArgumentException("Invalid opcode");
        }
    }
}
