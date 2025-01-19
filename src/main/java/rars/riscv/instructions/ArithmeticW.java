package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;
import rars.util.ConversionUtils;

public abstract class ArithmeticW extends BasicInstruction {
    private final @NotNull Arithmetic base;

    public ArithmeticW(
        final @NotNull String usage,
        final @NotNull String description,
        final @NotNull String funct7,
        final @NotNull String funct3,
        final @NotNull Arithmetic base
    ) {
        super(
            usage, description, BasicInstructionFormat.R_FORMAT,
            funct7 + " ttttt sssss " + funct3 + " fffff 0111011"
        );
        this.base = base;
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final long newValue = base.computeW(
            ConversionUtils.longLowerHalfToInt(context.registerFile().getLongValue(statement.getOperand(1))),
            ConversionUtils.longLowerHalfToInt(context.registerFile().getLongValue(statement.getOperand(2)))
        );
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
