package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float32;
import io.github.chr1sps.jsoftfloat.types.Float64;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FCVTDS class.</p>
 */
public class FCVTDS extends BasicInstruction {
    /**
     * <p>Constructor for FCVTDS.</p>
     */
    public FCVTDS() {
        super("fcvt.d.s f1, f2, dyn", "Convert a float to a double: Assigned the value of f2 to f1",
                BasicInstructionFormat.R4_FORMAT, "0100001 00000 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        Float32 in = new Float32(FloatingPointRegisterFile.getValue(operands[1]));
        Float64 out = new Float64(0);
        out = FCVTSD.convert(in, out, e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], out.bits);
    }
}
