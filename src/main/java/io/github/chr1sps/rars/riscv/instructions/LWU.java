package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;

/**
 * <p>LWU class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
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
    public long load(int address) throws AddressErrorException {
        return Globals.memory.getWord(address) & 0xFFFF_FFFFL;
    }
}
