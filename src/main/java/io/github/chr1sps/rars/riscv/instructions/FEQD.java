package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FEQD class.</p>
 */
public class FEQD extends BasicInstruction {
    /**
     * <p>Constructor for FEQD.</p>
     */
    public FEQD() {
        super("feq.d t1, f1, f2", "Floating EQuals (64 bit): if f1 = f2, set t1 to 1, else set t1 to 0",
                BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 010 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) {
        int[] operands = statement.getOperands();
        Float64 f1 = Double.getDouble(operands[1]), f2 = Double.getDouble(operands[2]);
        Environment e = new Environment();
        boolean result = io.github.chr1sps.jsoftfloat.operations.Comparisons.compareQuietEqual(f1, f2, e);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], result ? 1 : 0);
    }
}
