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

public final class FCVTDLU extends BasicInstruction {
    public static final FCVTDLU INSTANCE = new FCVTDLU();

    private FCVTDLU() {
        super(
            "fcvt.d.lu f1, t1, dyn", "Convert double from unsigned long: Assigns the second of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101001 00011 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 tmp = new Float64(0);
        final long value = RegisterFile.getValueLong(statement.getOperand(1));
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        final Float64 converted = Conversions.convertFromInt(unsigned, e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegister(statement.getOperand(0), converted.bits);
    }
}
