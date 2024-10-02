package rars.riscv.instructions;

import rars.Globals;
import rars.exceptions.AddressErrorException;

/**
 * <p>LD class.</p>
 *
 */
public class LD extends Load {
    /**
     * <p>Constructor for LD.</p>
     */
    public LD() {
        super("ld t1, -100(t2)", "Set t1 to contents of effective memory double word address", "011", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long load(final int address) throws AddressErrorException {
        return Globals.memory.getDoubleWord(address);
    }
}
