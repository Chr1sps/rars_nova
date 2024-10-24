package rars.riscv.instructions;

import rars.Globals;
import rars.exceptions.AddressErrorException;

/**
 * <p>SD class.</p>
 */
public final class SD extends Store {
    public static final SD INSTANCE = new SD();

    /**
     * <p>Constructor for SD.</p>
     */
    private SD() {
        super("sd t1, -100(t2)", "Store double word : Store contents of t1 into effective memory double word address",
                "011");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store(final int address, final long data) throws AddressErrorException {
        Globals.memory.setDoubleWord(address, data);
    }
}
