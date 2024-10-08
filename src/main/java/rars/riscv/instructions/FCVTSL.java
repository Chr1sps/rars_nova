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
 * <p>FCVTSL class.</p>
 */
public class FCVTSL extends BasicInstruction {
    /**
     * <p>Constructor for FCVTSL.</p>
     */
    public FCVTSL() {
        super("fcvt.s.l f1, t1, dyn", "Convert float from long: Assigns the second of t1 to f1",
                BasicInstructionFormat.I_FORMAT, "1101000 00010 sssss ttt fffff 1010011", true);
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
        final Float32 converted = Conversions
                .convertFromInt(BigInteger.valueOf(RegisterFile.getValueLong(operands[1])), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(operands[0], converted.bits);
    }
}
