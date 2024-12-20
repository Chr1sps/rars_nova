package rars.riscv.instructions;

import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

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
        return Memory.getInstance().getDoubleWord(address);
    }
}
