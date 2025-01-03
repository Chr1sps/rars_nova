package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;

public final class SRAI64 extends BasicInstruction {
    public static final @NotNull SRAI64 INSTANCE = new SRAI64();

    private SRAI64() {
        super(
            "srai t1,t2,33",
            "Shift right arithmetic : Set t1 to result of sign-extended shifting t2 right by number of bits specified" +
                " by immediate",
            BasicInstructionFormat.R_FORMAT, "010000 tttttt sssss 101 fffff 0010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        // Uses >> because sign fill
        final var shifted = RegisterFile.getValueLong(statement.getOperand(1)) >> statement.getOperand(2);
        RegisterFile.updateRegister(
            statement.getOperand(0),
            shifted
        );
    }
}
