package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FSGNJD extends BasicInstruction {
    public static final @NotNull FSGNJD INSTANCE = new FSGNJD();

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
    public void simulate(final @NotNull ProgramStatement statement, @NotNull final SimulationContext context) throws
        SimulationException {
        final long result = (context.fpRegisterFile().getLongValue(statement.getOperand(1)) & 0x7FFFFFFF_FFFFFFFFL) | (
            context.fpRegisterFile().getLongValue(statement.getOperand(2)) & 0x80000000_00000000L
        );
        context.fpRegisterFile().updateRegisterByNumber(statement.getOperand(0), result);
    }
}
