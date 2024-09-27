package rars.riscv.instructions;

import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import org.jetbrains.annotations.NotNull;

/**
 * <p>FSGNJND class.</p>
 */
public class FSGNJND extends BasicInstruction {
    /**
     * <p>Constructor for FSGNJND.</p>
     */
    public FSGNJND() {
        super("fsgnjn.d f1, f2, f3",
                "Floating point sign injection (inverted 64 bit):  replace the sign bit of f2 with the opposite of sign bit of f3 and assign it to f1",
                BasicInstructionFormat.R_FORMAT, "0010001 ttttt sssss 001 fffff 1010011");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(@NotNull final ProgramStatement statement) {
        final int[] operands = statement.getOperands();
        final long result = (FloatingPointRegisterFile.getValueLong(operands[1]) & 0x7FFFFFFF_FFFFFFFFL) |
                ((~FloatingPointRegisterFile.getValueLong(operands[2])) & 0x80000000_00000000L);
        FloatingPointRegisterFile.updateRegisterLong(operands[0], result);
    }
}
