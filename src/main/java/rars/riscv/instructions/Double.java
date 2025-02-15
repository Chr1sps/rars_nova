package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public abstract class Double extends BasicInstruction {
    protected Double(final String name, final String description, final String funct) {
        super(
            name + " f1, f2, f3, dyn", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss qqq fffff 1010011"
        );
    }

    protected Double(final String name, final String description, final String funct, final String rm) {
        super(
            name + " f1, f2, f3", description, BasicInstructionFormat.R_FORMAT,
            funct + "ttttt sssss " + rm + " fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final var environment = new Environment();
        final var hasRoundingMode = statement.hasOperand(3);
        if (hasRoundingMode) {
            environment.mode = Floating.getRoundingMode(statement.getOperand(3), statement, context.csrRegisterFile());
        }
        final Float64 result = compute(
            new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(1))),
            new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(2))), environment
        );
        Floating.setfflags(context.csrRegisterFile(), environment);
        context.fpRegisterFile().updateRegisterByNumber(statement.getOperand(0), result.bits);
    }

    public abstract @NotNull Float64 compute(@NotNull Float64 f1, @NotNull Float64 f2, @NotNull Environment e);
}
