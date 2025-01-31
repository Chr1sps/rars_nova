package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Conversions;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

import java.math.BigInteger;

public final class FCVTDLU extends BasicInstruction {
    public static final FCVTDLU INSTANCE = new FCVTDLU();

    private FCVTDLU() {
        super(
            "fcvt.d.lu f1, t1, dyn", "Convert double from unsigned long: Assigns the value of t1 to f1",
            BasicInstructionFormat.I_FORMAT, "1101001 00011 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile);
        final Float64 tmp = new Float64(0);
        final long value = context.registerFile.getLongValue(statement.getOperand(1));
        BigInteger unsigned = BigInteger.valueOf(value);
        if (value < 0) {
            unsigned = unsigned.add(BigInteger.ONE.shiftLeft(64));
        }
        final Float64 converted = Conversions.convertFromInt(unsigned, e, tmp);
        Floating.setfflags(context.csrRegisterFile, e);
        context.fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), converted.bits);
    }
}
