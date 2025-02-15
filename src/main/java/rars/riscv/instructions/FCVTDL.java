package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

import java.math.BigInteger;

public final class FCVTDL extends BasicInstruction {
    public static final FCVTDL INSTANCE = new FCVTDL();

    private FCVTDL() {
        super(
            "fcvt.d.l f1, t1, dyn", "Convert double from long: Assigns the value of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101001 00010 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile());
        final Float64 tmp = new Float64(0);
        final Float64 converted = Conversions
            .convertFromInt(BigInteger.valueOf(context.registerFile().getLongValue(statement.getOperand(1))), e, tmp);
        Floating.setfflags(context.csrRegisterFile(), e);
        context.fpRegisterFile().updateRegisterByNumber(statement.getOperand(0), converted.bits);
    }
}
