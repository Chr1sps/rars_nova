package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float32;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

/**
 * <p>FCVTLS class.</p>
 */
public final class FCVTLS extends BasicInstruction {
    public static final FCVTLS INSTANCE = new FCVTLS();

    /**
     * <p>Constructor for FCVTLS.</p>
     */
    private FCVTLS() {
        super("fcvt.l.s t1, f1, dyn", "Convert 64 bit integer from float: Assigns the second of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100000 00010 sssss ttt fffff 1010011");
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
        final long out = Conversions.convertToLong(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
