package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FLED extends BasicInstruction {
    public static final FLED INSTANCE = new FLED();

    private FLED() {
        super(
            "fle.d t1, f1, f2", "Floating Less than or Equals (64 bit): if f1 <= f2, set t1 to 1, else set t1 to 0",
            BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {

        final Float64 f1 = new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(1)));
        final Float64 f2 = new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(2)));
        final Environment e = new Environment();
        final boolean result = Comparisons.compareSignalingLessThanEqual(f1, f2, e);
        Floating.setfflags(context.csrRegisterFile(), e);
        final long newValue = result ? 1 : 0;
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
