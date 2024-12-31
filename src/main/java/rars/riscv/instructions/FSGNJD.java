package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public final class FSGNJD extends BasicInstruction {
    public static final FSGNJD INSTANCE = new FSGNJD();

    private FSGNJD() {
        super(
            "fsgnj.d f1, f2, f3",
            "Floating point sign injection (64 bit): replace the sign bit of f2 with the sign bit of f3 and assign it" +
                " to f1",
            BasicInstructionFormat.R_FORMAT,
            "0010001 ttttt sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) {
        final long result = (FloatingPointRegisterFile.getValueLong(statement.getOperand(1)) & 0x7FFFFFFF_FFFFFFFFL) |
            (FloatingPointRegisterFile.getValueLong(statement.getOperand(2)) & 0x80000000_00000000L);
        FloatingPointRegisterFile.updateRegisterLong(statement.getOperand(0), result);
    }
}
