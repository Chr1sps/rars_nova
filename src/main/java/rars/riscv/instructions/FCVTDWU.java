package rars.riscv.instructions;

import java.math.BigInteger;

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
 * <p>FCVTDWU class.</p>
 */
public class FCVTDWU extends BasicInstruction {
    /**
     * <p>Constructor for FCVTDWU.</p>
     */
    public FCVTDWU() {
        super("fcvt.d.wu f1, t1, dyn", "Convert double from unsigned integer: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101001 00001 sssss ttt fffff 1010011");
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
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValue(operands[1]) & 0xFFFFFFFFL), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], converted.bits);
    }
}