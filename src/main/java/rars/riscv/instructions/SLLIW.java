package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class SLLIW extends BasicInstruction {
    public static final @NotNull SLLIW INSTANCE = new SLLIW();

    private SLLIW() {
        super(
            "slliw t1,t2,10",
            "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by " +
                "immediate",
            BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0011011"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {
        // Copy from SLLI
        final long newValue = context.registerFile().getIntValue(statement.getOperand(1)) << statement.getOperand(
            2);
        context.registerFile().updateRegisterByNumber(statement.getOperand(0), newValue);
    }
}
