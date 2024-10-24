package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

/**
 * <p>FCVTLUD class.</p>
 */
public final class FCVTLUD extends BasicInstruction {
    public static final FCVTLUD INSTANCE = new FCVTLUD();

    /**
     * <p>Constructor for FCVTLUD.</p>
     */
    private FCVTLUD() {
        super("fcvt.lu.d t1, f1, dyn",
                "Convert unsigned 64 bit integer from double: Assigns the second of f1 (rounded) to t1",
                BasicInstructionFormat.I_FORMAT, "1100001 00011 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float64 in = new Float64(FloatingPointRegisterFile.getValueLong(operands[1]));
        final long out = Conversions.convertToUnsignedLong(in, e, false);
        Floating.setfflags(e);
        RegisterFile.updateRegister(operands[0], out);
    }
}
