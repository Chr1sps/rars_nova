package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

public final class FLTD extends BasicInstruction {
    public static final @NotNull FLTD INSTANCE = new FLTD();

    private FLTD() {
        super(
            "flt.d t1, f1, f2", "Floating Less Than (64 bit): if f1 < f2, set t1 to 1, else set t1 to 0",
            BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 001 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Float64 f1 = Double.getDouble(statement.getOperand(1));
        final Float64 f2 = Double.getDouble(statement.getOperand(2));
        final Environment e = new Environment();
        final boolean result = Comparisons.compareSignalingLessThan(f1, f2, e);
        Floating.setfflags(e);
        final long newValue = result ? 1 : 0;
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
