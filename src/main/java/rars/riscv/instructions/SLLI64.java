package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;

public final class SLLI64 extends BasicInstruction {
    public static final @NotNull SLLI64 INSTANCE = new SLLI64();

    private SLLI64() {
        super(
            "slli t1,t2,33",
            "Shift left logical : Set t1 to result of shifting t2 left by number of bits specified by immediate",
            BasicInstructionFormat.R_FORMAT, "000000 tttttt sssss 001 fffff 0010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws SimulationException {
        final long newValue = Globals.REGISTER_FILE.getLongValue(statement.getOperand(1)) << statement.getOperand(2);
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
