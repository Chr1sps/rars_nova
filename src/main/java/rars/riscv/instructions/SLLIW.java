package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.RegisterFile;

/**
 * <p>SLLIW class.</p>
 */
public final class SLLIW extends BasicInstruction {
    public static final SLLIW INSTANCE = new SLLIW();

    /**
     * <p>Constructor for SLLIW.</p>
     */
    private SLLIW() {
        super("slliw t1,t2,10",
                "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0011011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(@NotNull final ProgramStatement statement) {
        // Copy from SLLI
        final int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], (long) RegisterFile.getValue(operands[1]) << operands[2]);
    }
}
