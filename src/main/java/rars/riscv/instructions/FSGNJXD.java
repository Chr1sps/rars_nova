package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.SimulationContext;

public final class FSGNJXD extends BasicInstruction {
    public static final @NotNull FSGNJXD INSTANCE = new FSGNJXD();

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
    public void simulate(final @NotNull ProgramStatement statement, @NotNull SimulationContext context) throws
        SimulationException {
        final var f2 = Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(1));
        final var f3 = Globals.FP_REGISTER_FILE.getLongValue(statement.getOperand(2));
        final var result = (f2 & 0x7FFFFFFF_FFFFFFFFL) | ((f2 ^ f3) & 0x80000000_00000000L);
        Globals.FP_REGISTER_FILE.updateRegisterByNumber(statement.getOperand(0), result);
    }
}
