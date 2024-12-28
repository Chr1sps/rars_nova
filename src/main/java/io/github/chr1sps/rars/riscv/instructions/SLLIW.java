package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>SLLIW class.</p>
 */
public class SLLIW extends BasicInstruction {
    /**
     * <p>Constructor for SLLIW.</p>
     */
    public SLLIW() {
        super("slliw t1,t2,10",
                "Shift left logical (32 bit): Set t1 to result of shifting t2 left by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT, "0000000 ttttt sssss 001 fffff 0011011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull ProgramStatement statement) {
        // Copy from SLLI
        int[] operands = statement.getOperands();
        RegisterFile.updateRegister(operands[0], (long) RegisterFile.getValue(operands[1]) << operands[2]);
    }
}
