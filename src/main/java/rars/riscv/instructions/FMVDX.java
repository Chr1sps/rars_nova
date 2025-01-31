package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public final class FMVDX extends BasicInstruction {
    public static final @NotNull FMVDX INSTANCE = new FMVDX();

    private FMVDX() {
        super(
            "fmv.d.x f1, t1", "Move float: move bits representing a double from an 64 bit integer register",
            BasicInstructionFormat.I_FORMAT, "1111001 00000 sssss 000 fffff 1010011"
        );
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {

        context.fpRegisterFile.updateRegisterByNumber(
            statement.getOperand(0),
            context.registerFile.getLongValue(statement.getOperand(1))
        );
    }
}
