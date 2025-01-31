package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.simulator.SimulationContext;

public final class SRLIW extends BasicInstruction {
    public static final @NotNull SRLIW INSTANCE = new SRLIW();

    private SRLIW() {
        super(
            "srliw t1,t2,10",
            "Shift right logical (32 bit): Set t1 to result of shifting t2 right by number of bits specified by " +
                "immediate",
            BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 101 fffff 0011011"
        );
    }

    @Override
    public void simulateImpl(@NotNull final SimulationContext context, final @NotNull ProgramStatement statement) throws
        SimulationException {
        // Use the code directly from SRLI
        final long newValue = context.registerFile.getIntValue(statement.getOperand(1)) >>> statement.getOperand(2);
        context.registerFile.updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
