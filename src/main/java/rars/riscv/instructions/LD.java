package rars.riscv.instructions;

import rars.Globals;
import rars.exceptions.AddressErrorException;

/**
 * <p>LD class.</p>
 */
public final class LD extends Load {
    public static final LD INSTANCE = new LD();

    /**
     * <p>Constructor for LD.</p>
     */
    private LD() {
        super("ld t1, -100(t2)", "Set t1 to contents of effective memory double word address", "011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long load(final int address) throws AddressErrorException {
        return Globals.memory.getDoubleWord(address);
    }
}
