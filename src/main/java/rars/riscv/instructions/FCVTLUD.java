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

public final class FCVTLUD extends BasicInstruction {
    public static final FCVTLUD INSTANCE = new FCVTLUD();

    private FCVTLUD() {
        super(
            "fcvt.lu.d t1, f1, dyn",
            "Convert unsigned 64 bit integer from double: Assigns the second of f1 (rounded) to t1",
            BasicInstructionFormat.I_FORMAT, "1100001 00011 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 in = new Float64(Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(1)));
        final long out = Conversions.convertToUnsignedLong(in, e, false);
        Floating.setfflags(e);
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), out);
    }
}
