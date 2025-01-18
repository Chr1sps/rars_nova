package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
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
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
        try {
            final var value = Globals.MEMORY_INSTANCE.getDoubleWord(
                Globals.REGISTER_FILE.getIntValue(statement.getOperand(2))
                    + upperImmediate
            );
            Globals.FP_REGISTER_FILE.updateRegisterByNumber(
                statement.getOperand(0),
                value
            );
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
