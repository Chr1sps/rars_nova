package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

/**
 * <p>FSGNJXD class.</p>
 *
 */
public class FSGNJXD extends BasicInstruction {
    /**
     * <p>Constructor for FSGNJXD.</p>
     */
    public FSGNJXD() {
        super("fsgnjx.d f1, f2, f3",
                "Floating point sign injection (xor 64 bit):  xor the sign bit of f2 with the sign bit of f3 and assign it to f1",
                BasicInstructionFormat.R_FORMAT, "0010001 ttttt sssss 010 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        long f2 = FloatingPointRegisterFile.getValueLong(operands[1]),
                f3 = FloatingPointRegisterFile.getValueLong(operands[2]);
        long result = (f2 & 0x7FFFFFFF_FFFFFFFFL) | ((f2 ^ f3) & 0x80000000_00000000L);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result);
    }
}
