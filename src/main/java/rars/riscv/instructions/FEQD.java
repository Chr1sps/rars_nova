package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;
import rars.jsoftfloat.operations.Comparisons;
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
    @Override
    public void simulate(@NotNull final ProgramStatement statement) {
        final int[] operands = statement.getOperands();
        final Float64 f1 = Double.getDouble(operands[1]);
        final Float64 f2 = Double.getDouble(operands[2]);
        final Environment e = new Environment();
        final boolean result = Comparisons.compareQuietEqual(f1, f2, e);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], result ? 1 : 0);
    }
}
