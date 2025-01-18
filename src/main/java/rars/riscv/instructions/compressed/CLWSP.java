package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.hardware.registerFiles.RegisterFile;

/**
 * From RISC-V spec:
 * <p>
 * C.LWSP loads a 32-bit value from memory into register rd. It computes an effective address by adding
 * the zero-extended offset, scaled by 4, to the stack pointer, x2. It expands to lw rd, offset(x2).
 * C.LWSP is only valid when rd≠x0 the code points with rd=x0 are reserved.
 * </p>
 */
public final class CLWSP extends CompressedInstruction {
    public static final @NotNull CLWSP INSTANCE = new CLWSP();

    private CLWSP() {
        super(
            "c.lwsp t, offset",
            "Load word from a given offset from the stack pointer",
            CompressedInstructionFormat.CI,
            "010 s fffff ssss 10",
            (statement, context) -> {
                final var destinationRegister = statement.getOperand(0);
                assert isRVCRegister(destinationRegister) : "Destination register must be one of the ones supported " +
                    "by the C " +
                    "extension (x8-x15)";
                final var currentStackPointer = (int) context.registerFile()
                    .getIntValue(RegisterFile.STACK_POINTER_REGISTER_INDEX);
                final var offset = statement.getOperand(1) << 2;
                try {
                    final var address = currentStackPointer + offset;
                    final var data = context.memory().getWord(address);
                    context.registerFile().updateRegisterByNumber(destinationRegister, data);
                } catch (final AddressErrorException e) {
                    throw new SimulationException(statement, e);
                }
            }
        );
    }
}
