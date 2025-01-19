package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FCVTWUD extends BasicInstruction {
    public static final FCVTWUD INSTANCE = new FCVTWUD();

    private FCVTWUD() {
        super(
            "fcvt.wu.d t1, f1, dyn", "Convert unsinged integer from double: Assigns the second of f1 (rounded) to t1",
            BasicInstructionFormat.I_FORMAT, "1100001 00001 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final var environment = new Environment();
        environment.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile());
        final var input = new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(1)));
        final var output = Conversions.convertToUnsignedInt(input, environment, false);
        Floating.setfflags(context.csrRegisterFile(), environment);
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), output);
    }
}
