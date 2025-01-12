package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

public final class FMVXD extends BasicInstruction {
    public static final @NotNull FMVXD INSTANCE = new FMVXD();

    private FMVXD() {
        super(
            "fmv.x.d t1, f1", "Move double: move bits representing a double to an 64 bit integer register",
            BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {

        final long newValue = Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(1));
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
