package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.exceptions.AddressErrorException;

public final class LWU extends Load {
    public static final @NotNull LWU INSTANCE = new LWU();

    private LWU() {
        super("lwu t1, -100(t2)", "Set t1 to contents of effective memory word address without sign-extension", "110");
    }

    @Override
    public long load(final int address) throws AddressErrorException {
        return Integer.toUnsignedLong(Globals.MEMORY_INSTANCE.getWord(address));
    }
}
