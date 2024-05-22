package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>SRLIW class.</p>
 *
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
    public void simulate(ProgramStatement statement) {
        // Use the code directly from SRLI
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], RegisterFile.getValue(operands[1]) >>> operands[2]);
    }
}
