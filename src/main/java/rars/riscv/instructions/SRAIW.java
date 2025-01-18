package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class SRAIW extends BasicInstruction {
    public static final @NotNull SRAIW INSTANCE = new SRAIW();

    private SRAIW() {
        super(
            "sraiw t1,t2,10",
            "Shift right arithmetic (32 bit): Set t1 to result of sign-extended shifting t2 right by number of bits " +
                "specified by immediate",
            BasicInstructionFormat.R_FORMAT, "0100000 ttttt sssss 101 fffff 0011011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {
        // Use the code directly from SRAI
        final long newValue = Globals.REGISTER_FILE.getIntValue(statement.getOperand(1)) >> statement.getOperand(2);
        Globals.REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
