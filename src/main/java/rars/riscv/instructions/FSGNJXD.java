package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;

public final class FSGNJXD extends BasicInstruction {
    public static final FSGNJXD INSTANCE = new FSGNJXD();

    private FSGNJXD() {
        super(
            "fsgnjx.d f1, f2, f3",
            "Floating point sign injection (xor 64 bit):  xor the sign bit of f2 with the sign bit of f3 and assign " +
                "it to f1",
            BasicInstructionFormat.R_FORMAT,
            "0010001 ttttt sssss 010 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        final long f2 = FloatingPointRegisterFile.getValueLong(statement.getOperand(1));
        final long f3 = FloatingPointRegisterFile.getValueLong(statement.getOperand(2));
        final long result = (f2 & 0x7FFFFFFF_FFFFFFFFL) | ((f2 ^ f3) & 0x80000000_00000000L);
        FloatingPointRegisterFile.updateRegister(statement.getOperand(0), result);
    }
}
