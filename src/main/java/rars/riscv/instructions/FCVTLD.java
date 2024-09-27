package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float64;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import rars.jsoftfloat.operations.Conversions;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FCVTLD class.</p>
 */
public class FCVTLD extends BasicInstruction {
    /**
     * <p>Constructor for FCVTLD.</p>
     */
    public FCVTLD() {
        super("fcvt.l.d t1, f1, dyn", "Convert 64 bit integer from double: Assigns the value of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100001 00010 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        final long out = Conversions.convertToLong(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
