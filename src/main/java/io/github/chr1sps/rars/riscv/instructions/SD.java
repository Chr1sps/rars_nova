package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;

/**
 * <p>SD class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SD extends Store {
    /**
     * <p>Constructor for SD.</p>
     */
    public SD() {
        super("sd t1, -100(t2)", "Store double word : Store contents of t1 into effective memory double word address",
                "011", true);
    }

    /**
     * {@inheritDoc}
     */
    public void store(int address, long data) throws AddressErrorException {
        Globals.memory.setDoubleWord(address, data);
    }
}
