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

import java.math.BigInteger;

public final class FCVTSL extends BasicInstruction {
    public static final FCVTSL INSTANCE = new FCVTSL();

    private FCVTSL() {
        super(
            "fcvt.s.l f1, t1, dyn", "Convert float from long: Assigns the second of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101000 00010 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float32 tmp = new Float32(0);
        final Float32 converted = Conversions
            .convertFromInt(BigInteger.valueOf(RegisterFile.INSTANCE.getLongValue(statement.getOperand(1))), e, tmp);
        Floating.setfflags(e);
        FloatingPointRegisterFile.updateRegisterInt(statement.getOperand(0), converted.bits);
    }
}
