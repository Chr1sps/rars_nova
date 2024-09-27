package rars.riscv.instructions;

import rars.jsoftfloat.types.Float64;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FCLASSD class.</p>
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
    public void simulate(@NotNull final ProgramStatement statement) {
        final int[] operands = statement.getOperands();
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        FCLASSS.fclass(in, operands[0]);
    }
}
