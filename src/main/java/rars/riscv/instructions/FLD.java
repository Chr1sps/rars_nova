package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.Memory;
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
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {

        final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
        try {
            final long low = Memory.getInstance()
                .getWord(RegisterFile.getValue(statement.getOperand(2)) + upperImmediate);
            final long high = Memory.getInstance()
                .getWord(RegisterFile.getValue(statement.getOperand(2)) + upperImmediate + 4);
            FloatingPointRegisterFile.updateRegisterLong(statement.getOperand(0), (high << 32) | (low & 0xFFFFFFFFL));
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
