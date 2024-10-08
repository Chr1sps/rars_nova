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

import java.math.BigInteger;

/**
 * <p>FCVTDW class.</p>
 */
public class FCVTDW extends BasicInstruction {
    /**
     * <p>Constructor for FCVTDW.</p>
     */
    public FCVTDW() {
        super("fcvt.d.w f1, t1, dyn", "Convert double from integer: Assigns the second of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00000 sssss ttt fffff 1010011");
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
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValue(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}
