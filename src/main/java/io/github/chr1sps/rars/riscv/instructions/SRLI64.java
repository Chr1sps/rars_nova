package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>SRLI64 class.</p>
 */
public class SRLI64 extends BasicInstruction {
    /**
     * <p>Constructor for SRLI64.</p>
     */
    public SRLI64() {
        super("srli t1,t2,33",
                "Shift right logical : Set t1 to result of shifting t2 right by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "000000 tttttt sssss 101 fffff 0010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) {
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], RegisterFile.getValueLong(operands[1]) >>> operands[2]);
    }
}
