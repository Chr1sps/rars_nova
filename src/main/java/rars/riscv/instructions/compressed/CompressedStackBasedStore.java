package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.SimulationCallback;
import rars.util.BinaryUtils;

public final class CompressedStackBasedStore extends CompressedInstruction {
    //    public static final @NotNull CompressedStackBasedStore CSWSP = new CompressedStackBasedStore(
//
//    );
    private final @NotNull SimulationCallback callback;

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
            BinaryUtils.intToBinaryString(opcode, 3) + " sss ttt ss fff 00"
        );
        this.callback = callback;
        if (opcode < 0 || opcode > 0b111) {
            throw new IllegalArgumentException("Invalid opcode");
        }
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        callback.simulate(statement);
    }
}
