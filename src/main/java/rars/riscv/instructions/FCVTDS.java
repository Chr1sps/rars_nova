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
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float32 in = new Float32(FloatingPointRegisterFile.getValue(operands[1]));
        Float64 out = new Float64(0);
        out = FCVTSD.convert(in, out, e);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], out.bits);
    }
}
