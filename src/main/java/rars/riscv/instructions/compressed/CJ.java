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
 * C.J performs an unconditional control transfer. The offset is sign-extended and added to the pc to
 * form the jump target address. C.J can therefore target a ±2 KiB range. C.J expands to <code>jal x0, offset</code>.
 * </p>
 */
public final class CJ extends CompressedInstruction {
    public static final CompressedInstruction INSTANCE = new CJ();
    
    private CJ() {
        super("c.j target", "Compressed jump: Jump to statement at target address", CompressedInstructionFormat.CJ_FORMAT, "101 fffffffffff 00");
    }

    @Override
    public void simulate(@NotNull final ProgramStatement statement) throws SimulationException {
        final int[] operands = statement.getOperands();
        Utils.processJump(RegisterFile.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH + operands[0]);
    }
}
