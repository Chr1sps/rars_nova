package rars.riscv.instructions;

import rars.jsoftfloat.Environment;
import rars.jsoftfloat.types.Float32;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import rars.jsoftfloat.operations.Conversions;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FCVTLUS class.</p>
 */
public class FCVTLUS extends BasicInstruction {
    /**
     * <p>Constructor for FCVTLUS.</p>
     */
    public FCVTLUS() {
        super("fcvt.lu.s t1, f1, dyn",
                "Convert unsigned 64 bit integer from float: Assigns the second of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100000 00011 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float32 in = new Float32(FloatingPointRegisterFile.getValue(operands[1]));
        final long out = Conversions.convertToUnsignedLong(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
