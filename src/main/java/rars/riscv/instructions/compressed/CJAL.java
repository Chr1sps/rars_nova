package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstruction;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.hardware.RegisterFile;
import rars.util.Utils;

/**
 * From RISC-V spec:
 * <p>
 * C.JAL is an RV32C-only instruction that performs the same operation as C.J, but additionally writes
 * the address of the instruction following the jump (pc+2) to the link register, x1. C.JAL expands to <code>jal
 * x1, offset</code>.
 * </p>
 */
public final class CJAL extends CompressedInstruction {
    public static final CompressedInstruction INSTANCE = new CJAL();

    private CJAL() {
        super("c.jal offset", "Compressed jump and link: Set t1 to Program Counter (return address) then jump to " +
            "statement at target address", CompressedInstructionFormat.CJ, "001 fffffffffff 01");
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        Utils.processReturnAddress(1); // x1 is the link register
        Utils.processJump(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + operands[0]);
    }
}
