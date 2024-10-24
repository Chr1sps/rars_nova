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

/**
 * <p>FLD class.</p>
 */
public final class FLD extends BasicInstruction {
    public static final FLD INSTANCE = new FLD();

    /**
     * <p>Constructor for FLD.</p>
     */
    private FLD() {
        super("fld f1, -100(t1)", "Load a double from memory",
                BasicInstructionFormat.I_FORMAT, "ssssssssssss ttttt 011 fffff 0000111");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        operands[1] = (operands[1] << 20) >> 20;
        try {
            final long low = Globals.memory.getWord(RegisterFile.getValue(operands[2]) + operands[1]);
            final long high = Globals.memory.getWord(RegisterFile.getValue(operands[2]) + operands[1] + 4);
            FloatingPointRegisterFile.updateRegisterLong(operands[0], (high << 32) | (low & 0xFFFFFFFFL));
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
