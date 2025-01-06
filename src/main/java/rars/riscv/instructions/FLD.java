package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

public final class FLD extends BasicInstruction {
    public static final FLD INSTANCE = new FLD();

    private FLD() {
        super(
            "fld f1, -100(t1)", "Load a double from memory",
            BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 011 fffff 0000111"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
        try {
            final var value = Globals.MEMORY_INSTANCE.getDoubleWord(
                RegisterFile.INSTANCE.getIntValue(statement.getOperand(2))
                    + upperImmediate
            );
            FloatingPointRegisterFile.updateRegister(
                statement.getOperand(0),
                value
            );
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
