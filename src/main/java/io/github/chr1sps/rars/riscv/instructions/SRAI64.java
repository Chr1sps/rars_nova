package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>SRAI64 class.</p>
 *
 */
public class SRAI64 extends BasicInstruction {
    /**
     * <p>Constructor for SRAI64.</p>
     */
    public SRAI64() {
        super("srai t1,t2,33",
                "Shift right arithmetic : Set t1 to result of sign-extended shifting t2 right by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "010000 tttttt sssss 101 fffff 0010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], RegisterFile.getValueLong(operands[1]) >> operands[2]);
    }
}
