package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.jsoftfloat.types.Float64;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FCVTSD class.</p>
 */
public class FCVTSD extends BasicInstruction {
    /**
     * <p>Constructor for FCVTSD.</p>
     */
    public FCVTSD() {
        super("fcvt.s.d f1, f2, dyn", "Convert a double to a float: Assigned the value of f2 to f1",
                BasicInstructionFormat.R4_FORMAT, "0100000 00001 sssss ttt fffff 1010011");
    }

    /**
     * <p>convert.</p>
     *
     * @param toconvert   a D object
     * @param constructor a S object
     * @param e           a {@link Environment} object
     * @param <S>         a S class
     * @param <D>         a D class
     * @return a S object
     */
    public static <S extends rars.jsoftfloat.types.Floating<S>, D extends rars.jsoftfloat.types.Floating<D>> S convert(
            @NotNull final D toconvert, @NotNull final S constructor, final Environment e) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        Float32 out = new Float32(0);
        out = convert(in, out, e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], out.bits);
    }
}