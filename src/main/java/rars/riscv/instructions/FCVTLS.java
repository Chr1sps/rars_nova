package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float32;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public final class FCVTLS extends BasicInstruction {
    public static final FCVTLS INSTANCE = new FCVTLS();

    private FCVTLS() {
        super(
            "fcvt.l.s t1, f1, dyn", "Convert 64 bit integer from float: Assigns the second of f1 (rounded) to t1",
            BasicInstructionFormat.I_FORMAT, "1100000 00010 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile());
        final Float32 in = new Float32(context.fpRegisterFile().getIntValue(statement.getOperand(1)));
        final long out = Conversions.convertToLong(in, e, false);
        Floating.setfflags(context.csrRegisterFile(), e);
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), out);
    }
}
