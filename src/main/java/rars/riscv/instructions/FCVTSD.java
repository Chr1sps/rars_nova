package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FCVTSD extends BasicInstruction {
    public static final FCVTSD INSTANCE = new FCVTSD();

    private FCVTSD() {
        super(
            "fcvt.s.d f1, f2, dyn", "Convert a double to a float: Assigned the second of f2 to f1",
            BasicInstructionFormat.R4_FORMAT, "0100000 00001 sssss ttt fffff 1010011"
        );
    }

    public static <S extends rars.jsoftfloat.types.Floating<S>, D extends rars.jsoftfloat.types.Floating<D>> @NotNull S convert(
        @NotNull final D toconvert, @NotNull final S constructor, final @NotNull Environment e) {
        if (toconvert.isInfinite()) {
            return toconvert.isSignMinus() ? constructor.NegativeInfinity() : constructor.Infinity();
        }
        if (toconvert.isZero()) {
            return toconvert.isSignMinus() ? constructor.NegativeZero() : constructor.Zero();
        }
        if (toconvert.isNaN()) {
            return constructor.NaN();
        }
        return constructor.fromExactFloat(toconvert.toExactFloat(), e);
    }

    // Kindof a long type, but removes duplicate code and would make it easy for
    // quads to be implemented.

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile());
        final Float64 in = new Float64(context.fpRegisterFile().getLongValue(statement.getOperand(1)));
        Float32 out = new Float32(0);
        out = convert(in, out, e);
        Floating.setfflags(context.csrRegisterFile(), e);
        context.fpRegisterFile().updateRegisterByNumberInt(statement.getOperand(0), out.bits);
    }
}
