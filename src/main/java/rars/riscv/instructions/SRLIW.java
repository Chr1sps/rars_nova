package rars.riscv.instructions;

import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>SRLIW class.</p>
 */
public class SRLIW extends BasicInstruction {
    /**
     * <p>Constructor for SRLIW.</p>
     */
    public SRLIW() {
        super("srliw t1,t2,10",
                "Shift right logical (32 bit): Set t1 to result of shifting t2 right by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0011011", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) {
        // Use the code directly from SRLI
        final int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], RegisterFile.getValue(operands[1]) >>> operands[2]);
    }
}
