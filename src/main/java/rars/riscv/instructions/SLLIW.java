package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;

public final class SLLIW extends BasicInstruction {
    public static final SLLIW INSTANCE = new SLLIW();

    private SLLIW() {
        super(
            "slliw t1,t2,10",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by " +
                "immediate",
            BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0011011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        // Copy from SLLI
        final long newValue = (long) (int) RegisterFile.INSTANCE.getIntValue(statement.getOperand(1)) << statement.getOperand(
            2);
        RegisterFile.INSTANCE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
