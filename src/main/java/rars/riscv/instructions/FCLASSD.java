package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public final class FCLASSD extends BasicInstruction {
    public static final FCLASSD INSTANCE = new FCLASSD();

    private FCLASSD() {
        super(
            "fclass.d t1, f1", "Classify a floating point number (64 bit)",
            BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 001 fffff 1010011"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) {
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(statement.getOperand(1)));
        FCLASSS.fclass(in, statement.getOperand(0));
    }
}
