package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.InstructionSet;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>SRAIW class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SRAIW extends BasicInstruction {
    /**
     * <p>Constructor for SRAIW.</p>
     */
    public SRAIW() {
        super("sraiw t1,t2,10",
                "Shift right arithmetic (32 bit): Set t1 to result of sign-extended shifting t2 right by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "0100000 ttttt sssss 101 fffff 0011011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        // Use the code directly from SRAI
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], RegisterFile.getValue(operands[1]) >> operands[2]);
    }
}
