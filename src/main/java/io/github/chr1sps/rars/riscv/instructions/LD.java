package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;

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
    public long load(int address) throws AddressErrorException {
        return Globals.memory.getDoubleWord(address);
    }
}
