package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FLD extends BasicInstruction {
    public static final FLD INSTANCE = new FLD();

    private FLD() {
        super(
            "fld f1, -100(t1)", "Load a double from memory",
            BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 011 fffff 0000111"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {

        final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
        try {
            final var value = context.memory().getDoubleWord(
                context.registerFile().getIntValue(statement.getOperand(2))
                    + upperImmediate
            );
            context.fpRegisterFile().updateRegisterByNumber(
                statement.getOperand(0),
                value
            );
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
