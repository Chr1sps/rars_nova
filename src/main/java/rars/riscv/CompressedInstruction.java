package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

public abstract non-sealed class CompressedInstruction extends Instruction implements SimullationCallback {
    protected static final int COMPRESSED_INSTRUCTION_LENGTH = 2;

    public final @NotNull CompressedInstructionFormat instructionFormat;
    private final @NotNull String operationMask;
    private final @NotNull SimullationCallback callback;

    protected CompressedInstruction(
        final @NotNull String example,
        final @NotNull String description,
        final @NotNull CompressedInstructionFormat instrFormat,
        final @NotNull String operMask,
        final @NotNull SimullationCallback callback
    ) {
        super(example, description);
        this.callback = callback;
        this.instructionFormat = instrFormat;
        this.operationMask = operMask.replaceAll(" ", "");
        assert this.operationMask.length() == 16 : "`%s` compressed instruction mask (%s) not 16 bits!".formatted(
            example,
            this.operationMask
        );
    }

    protected static boolean isRVCRegister(final int register) {
        return register >= 8 && register <= 15;
    }

    @Override
    public int getInstructionLength() {
        return COMPRESSED_INSTRUCTION_LENGTH;
    }

    @Override
    public final void simulate(
        final @NotNull ProgramStatement statement,
        final @NotNull SimulationContext context
    ) throws SimulationException {
        callback.simulate(statement, context);
    }
}
