package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FCVTWD extends BasicInstruction {
    public static final FCVTWD INSTANCE = new FCVTWD();

    private FCVTWD() {
        super(
            "fcvt.w.d t1, f1, dyn", "Convert integer from double: Assigns the second of f1 (rounded) to t1",
            BasicInstructionFormat.I_FORMAT, "1100001 00000 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 in = new Float64(Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(1)));
        final int out = Conversions.convertToInt(in, e, false);
        Floating.setfflags(e);
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), out);
    }
}
