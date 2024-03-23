package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;

public class FCVTSD extends BasicInstruction {
    public FCVTSD() {
        super("fcvt.s.d f1, f2, dyn", "Convert a double to a float: Assigned the value of f2 to f1",
                BasicInstructionFormat.R4_FORMAT, "0100000 00001 sssss ttt fffff 1010011");
    }

    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        Float32 out = new Float32(0);
        out = convert(in, out, e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], out.bits);
    }

    // Kindof a long type, but removes duplicate code and would make it easy for
    // quads to be implemented.
    public static <S extends io.github.chr1sps.jsoftfloat.types.Floating<S>, D extends io.github.chr1sps.jsoftfloat.types.Floating<D>> S convert(
            D toconvert, S constructor, Environment e) {
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
}