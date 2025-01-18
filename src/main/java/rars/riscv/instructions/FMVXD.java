package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FMVXD extends BasicInstruction {
    public static final @NotNull FMVXD INSTANCE = new FMVXD();

    private FMVXD() {
        super(
            "fmv.x.d t1, f1", "Move double: move bits representing a double to an 64 bit integer register",
            BasicInstructionFormat.I_FORMAT, "1110001 00000 sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {

        final long newValue = context.fpRegisterFile().getLongValue(statement.getOperand(1));
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
