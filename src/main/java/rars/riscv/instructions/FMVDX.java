package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

public final class FMVDX extends BasicInstruction {
    public static final FMVDX INSTANCE = new FMVDX();

    private FMVDX() {
        super(
            "fmv.d.x f1, t1", "Move float: move bits representing a double from an 64 bit integer register",
            BasicInstructionFormat.I_FORMAT, "1111001 00000 sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {

        FloatingPointRegisterFile.updateRegister(
            statement.getOperand(0),
            RegisterFile.getValueLong(statement.getOperand(1))
        );
    }
}
