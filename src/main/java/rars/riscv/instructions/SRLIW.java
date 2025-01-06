package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;

public final class SRLIW extends BasicInstruction {
    public static final SRLIW INSTANCE = new SRLIW();

    private SRLIW() {
        super(
            "srliw t1,t2,10",
            "Shift right logical (32 bit): Set t1 to result of shifting t2 right by number of bits specified by " +
                "immediate",
            BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0011011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        // Use the code directly from SRLI
        final long newValue = RegisterFile.INSTANCE.getIntValue(statement.getOperand(1)) >>> statement.getOperand(2);
        RegisterFile.INSTANCE.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
