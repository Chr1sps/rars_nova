package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

public abstract non-sealed class CompressedInstruction extends Instruction implements SimulationCallback {
    protected static final int COMPRESSED_INSTRUCTION_LENGTH = 2;

    public final @NotNull CompressedInstructionFormat instructionFormat;
    private final @NotNull String operationMask;
    private final @NotNull SimulationCallback callback;

    protected CompressedInstruction(
        final @NotNull String example,
        final @NotNull String description,
        final @NotNull CompressedInstructionFormat instrFormat,
        final @NotNull String operMask,
        final @NotNull SimulationCallback callback
    ) {
        super(example, description);
        this.callback = callback;
        this.instructionFormat = instrFormat;
        this.operationMask = operMask.replaceAll(" ", "");
        assert this.operationMask.length() == 16 : "`" + example + "` compressed instruction mask not 16 bits!";
    }


    protected static boolean isRVCRegister(final int register) {
        return register >= 8 && register <= 15;
    }

    @Override
    public int getInstructionLength() {
        return COMPRESSED_INSTRUCTION_LENGTH;
    }

    @Override
    public final void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        callback.simulate(statement);
    }
}
