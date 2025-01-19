package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public abstract class FusedDouble extends BasicInstruction {
    public FusedDouble(final String usage, final String description, final String op) {
        super(
            usage + ", dyn", description, BasicInstructionFormat.R4_FORMAT,
            "qqqqq 01 ttttt sssss " + "ppp" + " fffff 100" + op + "11"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(4), statement, context.csrRegisterFile());
        final Float64 result = compute(
            new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(1))),
            new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(2))),
            new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(3))), e
        );
        Floating.setfflags(context.csrRegisterFile(), e);
        context.fpRegisterFile().updateRegisterByNumber(statement.getOperand(0), result.bits);
    }

    protected abstract Float64 compute(Float64 r1, Float64 r2, Float64 r3, Environment e);
}
