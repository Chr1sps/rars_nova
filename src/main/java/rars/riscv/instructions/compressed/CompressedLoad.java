package rars.riscv.instructions.compressed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;
import rars.exceptions.AddressErrorException;
import rars.exceptions.SimulationException;
import rars.riscv.CompressedInstruction;
import rars.riscv.CompressedInstructionFormat;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;

import java.util.List;

public final class CompressedLoad extends CompressedInstruction {
    // TODO: Make sure the callbacks work properly
    public static final @NotNull CompressedLoad CLW = new CompressedLoad(
        "c.lw t1, -100(t2)",
        "Set t1 to contents of effective memory word address",
        0b010,
        RegisterFile::updateRegister,
        address -> Globals.MEMORY_INSTANCE.getWord(address)
    );
    public static final @NotNull CompressedLoad CFLW = new CompressedLoad(
        "c.flw f1, -100(t1)",
        "Load a float from memory",
        0b001,
        FloatingPointRegisterFile::updateRegisterLong,
        address -> Globals.MEMORY_INSTANCE.getWord(address)
    );
    public static final @NotNull CompressedLoad CLD = new CompressedLoad(
        "c.ld t1, -100(t2)",
        "Set t1 to contents of effective memory double word address",
        0b001,
        RegisterFile::updateRegister,
        address -> Globals.MEMORY_INSTANCE.getDoubleWord(address)
    );
    public static final @NotNull CompressedLoad CFLD = new CompressedLoad(
        "c.fld f1, -100(t1)",
        "Load a double from memory",
        0b001,
        FloatingPointRegisterFile::updateRegisterLong,
        address -> Globals.MEMORY_INSTANCE.getDoubleWord(address)
    );

    public static final @NotNull
    @Unmodifiable List<@NotNull CompressedLoad> ALL_INSTRUCTIONS = List.of(
        CLW,
        CFLW,
        CLD,
        CFLD
    );

    private CompressedLoad(
        final @NotNull String example,
        final @NotNull String description,
        final int opcode,
        final @NotNull UpdateCallback updateCallback,
        final @NotNull LoadCallback loadCallback
    ) {
        super(
            example,
            description,
            CompressedInstructionFormat.CL,
            BinaryUtils.intToBinaryString(opcode, 3) + " sss ttt ss fff 00",
            statement -> {
                final var upperImmediate = (statement.getOperand(1) << 20) >> 20;
                try {
                    updateCallback.update(
                        statement.getOperand(0),
                        loadCallback.load(RegisterFile.getValue(statement.getOperand(2)) + upperImmediate)
                    );
                } catch (final AddressErrorException e) {
                    throw new SimulationException(statement, e);
                }
            }
        );
        if (opcode < 0 || opcode > 0b111) {
            throw new IllegalArgumentException("Invalid opcode");
        }
    }

    @FunctionalInterface
    private interface LoadCallback {
        long load(int address) throws AddressErrorException;
    }

    @FunctionalInterface
    private interface UpdateCallback {
        void update(int address, long value);
    }
}
