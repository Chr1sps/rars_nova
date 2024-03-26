package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/**
 * <p>FLTD class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class FLTD extends BasicInstruction {
    /**
     * <p>Constructor for FLTD.</p>
     */
    public FLTD() {
        super("flt.d t1, f1, f2", "Floating Less Than (64 bit): if f1 < f2, set t1 to 1, else set t1 to 0",
                BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 001 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        Float64 f1 = Double.getDouble(operands[1]), f2 = Double.getDouble(operands[2]);
        Environment e = new Environment();
        boolean result = io.github.chr1sps.jsoftfloat.operations.Comparisons.compareSignalingLessThan(f1, f2, e);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], result ? 1 : 0);
    }
}
