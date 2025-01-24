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

public final class FCVTDW extends BasicInstruction {
    public static final FCVTDW INSTANCE = new FCVTDW();

    private FCVTDW() {
        super(
            "fcvt.d.w f1, t1, dyn", "Convert double from integer: Assigns the second of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101001 00000 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile());
        final Float64 tmp = new Float64(0);
        final Float64 converted = Conversions
            .convertFromInt(BigInteger.valueOf(context.registerFile().getIntValue(statement.getOperand(1))), e, tmp);
        Floating.setfflags(context.csrRegisterFile(), e);
        context.fpRegisterFile().updateRegisterByNumber(statement.getOperand(0), converted.bits);
    }
}
