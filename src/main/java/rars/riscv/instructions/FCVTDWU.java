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

public final class FCVTDWU extends BasicInstruction {
    public static final FCVTDWU INSTANCE = new FCVTDWU();

    private FCVTDWU() {
        super(
            "fcvt.d.wu f1, t1, dyn", "Convert double from unsigned integer: Assigns the second of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101001 00001 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 tmp = new Float64(0);
        final Float64 converted = Conversions
            .convertFromInt(BigInteger.valueOf(RegisterFile.getValue(statement.getOperand(1)) & 0xFFFFFFFFL), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterLong(statement.getOperand(0), converted.bits);
    }
}
