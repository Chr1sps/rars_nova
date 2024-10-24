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

import java.math.BigInteger;

/**
 * <p>FCVTDL class.</p>
 */
public final class FCVTDL extends BasicInstruction {
    public static final FCVTDL INSTANCE = new FCVTDL();

    /**
     * <p>Constructor for FCVTDL.</p>
     */
    private FCVTDL() {
        super("fcvt.d.l f1, t1, dyn", "Convert double from long: Assigns the second of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00010 sssss ttt fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float64 tmp = new Float64(0);
        final Float64 converted = Conversions
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValueLong(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}
