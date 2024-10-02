package rars.riscv.instructions;

import java.math.BigInteger;

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
 * <p>FCVTSLU class.</p>
 */
public class FCVTSLU extends BasicInstruction {
    /**
     * <p>Constructor for FCVTSLU.</p>
     */
    public FCVTSLU() {
        super("fcvt.s.lu f1, t1, dyn", "Convert float from unsigned long: Assigns the value of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101000 00011 sssss ttt fffff 1010011", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(operands[2], statement);
        final Float32 tmp = new Float32(0);
        final long value = RegisterFile.getValueLong(operands[1]);
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        final Float32 converted = Conversions.convertFromInt(unsigned, e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], converted.bits);
    }
}
