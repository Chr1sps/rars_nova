package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float32;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

import java.math.BigInteger;

public final class FCVTSLU extends BasicInstruction {
    public static final FCVTSLU INSTANCE = new FCVTSLU();

    private FCVTSLU() {
        super(
            "fcvt.s.lu f1, t1, dyn", "Convert float from unsigned long: Assigns the second of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101000 00011 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float32 tmp = new Float32(0);
        final long value = Globals.REGISTER_FILE.getLongValue(statement.getOperand(1));
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        final Float32 converted = Conversions.convertFromInt(unsigned, e, tmp);
        Floating.setfflags(e);
        Globals.FP_REGISTER_FILE.updateRegisterByNumberInt(statement.getOperand(0), converted.bits);
    }
}
