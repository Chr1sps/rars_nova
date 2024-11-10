package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.SimulationException;
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
public class CJAL extends CompressedInstruction {
    private CJAL() {
        super("c.jal t1, target", "Compressed jump and link: Set t1 to Program Counter (return address) then jump to statement at target address", CompressedInstructionFormat.CJ_FORMAT, "001 fffffffffff 01");
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final var operands = statement.getOperands();
        Utils.processReturnAddress(operands[0]);
        Utils.processJump(RegisterFile.getProgramCounter() - getInstructionLength() + operands[1]);
    }
}
