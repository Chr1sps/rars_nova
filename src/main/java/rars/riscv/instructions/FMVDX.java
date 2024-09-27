package rars.riscv.instructions;

import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FMVDX class.</p>
 */
public class FMVDX extends BasicInstruction {
    /**
     * <p>Constructor for FMVDX.</p>
     */
    public FMVDX() {
        super("fmv.d.x f1, t1", "Move float: move bits representing a double from an 64 bit integer register",
                BasicInstructionFormat.I_FORMAT, "1111001 00000 sssss 000 fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull final ProgramStatement statement) {
        final int[] operands = statement.getOperands();
        FloatingPointRegisterFile.updateRegisterLong(operands[0], RegisterFile.getValueLong(operands[1]));
    }
}
