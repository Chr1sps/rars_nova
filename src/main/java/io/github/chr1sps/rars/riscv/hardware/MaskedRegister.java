package io.github.chr1sps.rars.riscv.hardware;

/**
 * A register which aliases a subset of another register
 *
 */
public class MaskedRegister extends Register {
    private final long mask;

    /**
     * <p>Constructor for MaskedRegister.</p>
     *
     * @param name the name to assign
     * @param num  the number to assign
     * @param val  the reset value
     * @param mask the bits to use
     */
    public MaskedRegister(String name, int num, long val, long mask) {
        super(name, num, val); // reset value does not matter
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized long setValue(long val) {
        long current = getValue();
        super.setValue((current & mask) | (val & ~mask));
        return current;
    }
}
