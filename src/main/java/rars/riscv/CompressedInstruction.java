package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;

public abstract non-sealed class CompressedInstruction extends Instruction {
    protected static final int COMPRESSED_INSTRUCTION_LENGTH = 2;

    private final CompressedInstructionFormat instructionFormat;
    private final @NotNull String operationMask;

    private final int opcodeMask; // integer with 1's where constants required (0/1 become 1, f/s/t become 0)
    private final int opcodeMatch; // integer matching constants required (0/1 become 0/1, f/s/t become 0)

    protected CompressedInstruction(final @NotNull String example,
                                    final @NotNull String description,
                                    final @NotNull CompressedInstructionFormat instrFormat,
                                    final @NotNull String operMask) {
        super(example, description);
        this.instructionFormat = instrFormat;
        this.operationMask = operMask;
        assert operMask.length() == 16 : "`" + example + "` compressed instruction mask not 16 bits!";

        this.opcodeMask = (int) Long.parseLong(this.operationMask.replaceAll("[01]", "1").replaceAll("[^01]", "0"), 2);
        this.opcodeMatch = (int) Long.parseLong(this.operationMask.replaceAll("[^01]", "0"), 2);
    }

    protected static boolean isRVCRegister(final int register) {
        return register >= 8 && register <= 15;
    }

    @Override
    public int getInstructionLength() {
        return COMPRESSED_INSTRUCTION_LENGTH;
    }

    public abstract void simulate(final @NotNull ProgramStatement statement) throws SimulationException;
}
