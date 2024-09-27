package rars.riscv.instructions;

import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FSD class.</p>
 */
public class FSD extends BasicInstruction {
    /**
     * <p>Constructor for FSD.</p>
     */
    public FSD() {
        super("fsd f1, -100(t1)", "Store a double to memory",
                BasicInstructionFormat.S_FORMAT, "sssssss fffff ttttt 011 sssss 0100111");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        operands[1] = (operands[1] << 20) >> 20;
        try {
            Globals.memory.setDoubleWord(RegisterFile.getValue(operands[2]) + operands[1],
                    FloatingPointRegisterFile.getValueLong(operands[0]));
        } catch (final AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }
}
