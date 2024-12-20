package rars.riscv.instructions;

import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

/**
 * <p>LWU class.</p>
 */
public final class LWU extends Load {
    public static final Load INSTANCE = new LWU();

    /**
     * <p>Constructor for LWU.</p>
     */
    private LWU() {
        super("lwu t1, -100(t2)", "Set t1 to contents of effective memory word address without sign-extension", "110");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long load(final int address) throws AddressErrorException {
        return Memory.getInstance().getWord(address) & 0xFFFF_FFFFL;
    }
}
