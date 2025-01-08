package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.jsoftfloat.Environment;
import rars.jsoftfloat.operations.Arithmetic;
import rars.jsoftfloat.types.Float64;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

public final class FSQRTD extends BasicInstruction {
    public static final FSQRTD INSTANCE = new FSQRTD();

    private FSQRTD() {
        super(
            "fsqrt.d f1, f2, dyn", "Floating SQuare RooT (64 bit): Assigns f1 to the square root of f2",
            BasicInstructionFormat.I_FORMAT, "0101101 00000 sssss ttt fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        final Environment e = new Environment();
        e.mode = Floating.getRoundingMode(statement.getOperand(2), statement);
        final Float64 result = Arithmetic
            .squareRoot(new Float64(Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(1))), e);
        Floating.setfflags(e);
        Globals.FP_REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), result.bits);
    }
}
