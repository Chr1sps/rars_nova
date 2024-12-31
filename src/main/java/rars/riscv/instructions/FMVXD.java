package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

public final class FMVXD extends BasicInstruction {
    public static final FMVXD INSTANCE = new FMVXD();

    private FMVXD() {
        super(
            "fmv.x.d t1, f1", "Move double: move bits representing a double to an 64 bit integer register",
            BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) {

        RegisterFile.updateRegister(
            statement.getOperand(0),
            FloatingPointRegisterFile.getValueLong(statement.getOperand(1))
        );
    }
}
