package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Comparisons;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public final class FEQD extends BasicInstruction {
    public static final FEQD INSTANCE = new FEQD();

    private FEQD() {
        super(
            "feq.d t1, f1, f2", "Floating EQuals (64 bit): if f1 = f2, set t1 to 1, else set t1 to 0",
            BasicInstructionFormat.R_FORMAT, "1010001 ttttt sssss 010 fffff 1010011"
        );
    }

    @Override
    public void simulate(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {

        final Float64 f1 = new Float64(context.fpRegisterFile.getLongValue(statement.getOperand(1)));
        final Float64 f2 = new Float64(context.fpRegisterFile.getLongValue(statement.getOperand(2)));
        final Environment e = new Environment();
        final boolean result = Comparisons.compareQuietEqual(f1, f2, e);
        Floating.setfflags(context.csrRegisterFile, e);
        final long newValue = result ? 1 : 0;
        context.registerFile.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
