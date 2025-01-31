package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public final class FSQRTD extends BasicInstruction {
    public static final @NotNull FSQRTD INSTANCE = new FSQRTD();

    private FSQRTD() {
        super(
            "fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
            BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement, context.csrRegisterFile);
        final Float64 result = Arithmetic
            .squareRoot(new Float64(context.fpRegisterFile.getLongValue(statement.getOperand(1))), e);
        Floating.setfflags(context.csrRegisterFile, e);
        context.fpRegisterFile.updateRegisterByNumber(statement.getOperand(0), result.bits);
    }
}
