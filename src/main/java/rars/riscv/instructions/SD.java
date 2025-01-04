package rars.riscv.instructions;

import rars.Globals;
import rars.exceptions.AddressErrorException;

public final class SD extends Store {
    public static final SD INSTANCE = new SD();

    private SD() {
        super("sd t1, -100(t2)", "Store double word : Store contents of t1 into effective memory double word address",
                "011");
    }

    @Override
    public void store(final int address, final long data) throws AddressErrorException {
        Globals.MEMORY_INSTANCE.setDoubleWord(address, data);
    }
}
