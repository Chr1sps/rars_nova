package rars.riscv.instructions;

import rars.Globals;
import rars.exceptions.AddressErrorException;

/**
 * <p>LWU class.</p>
 */
public class LWU extends Load {
    /**
     * <p>Constructor for LWU.</p>
     */
    public LWU() {
        super("lwu t1, -100(t2)", "Set t1 to contents of effective memory word address without sign-extension", "110",
                true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long load(final int address) throws AddressErrorException {
        return Globals.memory.getWord(address) & 0xFFFF_FFFFL;
    }
}
