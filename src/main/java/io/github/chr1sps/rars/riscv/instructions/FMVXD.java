package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>FMVXD class.</p>
 *
 */
public class FMVXD extends BasicInstruction {
    /**
     * <p>Constructor for FMVXD.</p>
     */
    public FMVXD() {
        super("fmv.x.d t1, f1", "Move double: move bits representing a double to an 64 bit integer register",
                BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 000 fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], FloatingPointRegisterFile.getValueLong(operands[1]));
    }
}
