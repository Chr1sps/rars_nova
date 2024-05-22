package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

/**
 * <p>FCLASSD class.</p>
 *
 */
public class FCLASSD extends BasicInstruction {
    /**
     * <p>Constructor for FCLASSD.</p>
     */
    public FCLASSD() {
        super("fclass.d t1, f1", "Classify a floating point number (64 bit)",
                BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 001 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        int[] operands = statement.getOperands();
        Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        FCLASSS.fclass(in, operands[0]);
    }
}
