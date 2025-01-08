package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

public final class FSD extends BasicInstruction {
    public static final FSD INSTANCE = new FSD();

    private FSD() {
        super(
            "fsd f1, -100(t1)", "Store a double to memory",
            BasicInstructionFormat.S_FORMAT, "sssssss fffff ttttt 011 sssss 0100111"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
        try {
            Globals.MEMORY_INSTANCE.setDoubleWord(
                Globals.REGISTER_FILE.getIntValue(statement.getOperand(2)) + upperImmediate,
                Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(0))
            );
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
