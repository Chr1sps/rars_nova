package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

public final class LD extends Load {
    public static final @NotNull LD INSTANCE = new LD();

    private LD() {
        super("ld t1, -100(t2)", "Set t1 to contents of effective memory double word address", "011");
    }

    @Override
    public long load(final int address, @NotNull Memory memory) throws AddressErrorException {
        return memory.getDoubleWord(address);
    }
}
