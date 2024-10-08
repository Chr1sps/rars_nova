package rars.riscv.hardware;

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
     * @param val  the reset second
     * @param mask the bits to use
     */
    public MaskedRegister(final String name, final int num, final long val, final long mask) {
        super(name, num, val); // reset second does not matter
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long setValue(final long val) {
        final long current = getValue();
        super.setValue((current & mask) | (val & ~mask));
        return current;
    }
}
